package com.testround.seven_eleven.domain.order;

import com.testround.seven_eleven.common.exception.BusinessException;
import com.testround.seven_eleven.domain.category.Category;
import com.testround.seven_eleven.domain.order.dto.*;
import com.testround.seven_eleven.domain.product.Product;
import com.testround.seven_eleven.domain.product.ProductRepository;
import com.testround.seven_eleven.domain.topping.Topping;
import com.testround.seven_eleven.domain.topping.ToppingRepository;
import com.testround.seven_eleven.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderDto createOrder(User currentUser, OrderRequest request) {
        try {
            // 1. Sinh mã đơn hàng sử dụng daily sequence
            Long seqVal = orderRepository.getNextOrderSequenceValue();
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String orderCode = String.format("OD-%s-%04d", datePart, seqVal);

            Order order = new Order();
            order.setOrderCode(orderCode);
            order.setUser(currentUser);
            order.setNote(request.getNote());
            order.setStatus(OrderStatus.PENDING);

            BigDecimal totalAmount = BigDecimal.ZERO;

            for (OrderItemRequest itemReq : request.getItems()) {
                // 2. Load và validate sản phẩm
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Sản phẩm không tồn tại"));

                if (!product.getIsActive()) {
                    throw new BusinessException("PRODUCT_INACTIVE", "Sản phẩm đã ngừng kinh doanh: " + product.getName());
                }

                // 3. Kiểm tra số lượng tồn kho
                if (product.getStockQuantity() < itemReq.getQuantity()) {
                    throw new BusinessException("OUT_OF_STOCK", "Sản phẩm " + product.getName() + " không đủ tồn kho. Tồn hiện tại: " + product.getStockQuantity());
                }

                // 4. Giảm tồn kho (Optimistic Lock hoạt động dựa trên @Version của thực thể Product)
                product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
                productRepository.save(product);

                // 5. Tính toán tiền Topping
                BigDecimal toppingsExtraPrice = BigDecimal.ZERO;
                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setProductName(product.getName());
                orderItem.setUnitPrice(product.getPrice());
                orderItem.setQuantity(itemReq.getQuantity());

                if (itemReq.getToppingIds() != null && !itemReq.getToppingIds().isEmpty()) {
                    // Xác định Root Category ID của sản phẩm để validate toppings
                    Category category = product.getCategory();
                    UUID rootCategoryId = category.getParent() != null ? category.getParent().getId() : category.getId();

                    // Tìm danh sách Topping hợp lệ của Root Category
                    List<Topping> activeToppings = toppingRepository.findActiveToppingsByCategoryId(rootCategoryId);
                    Map<UUID, Topping> toppingMap = activeToppings.stream()
                            .collect(Collectors.toMap(Topping::getId, Function.identity()));

                    for (UUID toppingId : itemReq.getToppingIds()) {
                        Topping topping = toppingMap.get(toppingId);
                        if (topping == null) {
                            throw new BusinessException("INVALID_TOPPING", "Topping không hợp lệ hoặc không thuộc danh mục của sản phẩm: " + toppingId);
                        }

                        // Snapshot topping
                        OrderItemTopping orderItemTopping = new OrderItemTopping();
                        orderItemTopping.setTopping(topping);
                        orderItemTopping.setToppingName(topping.getName());
                        orderItemTopping.setExtraPrice(topping.getExtraPrice());

                        orderItem.addOrderItemTopping(orderItemTopping);
                        toppingsExtraPrice = toppingsExtraPrice.add(topping.getExtraPrice());
                    }
                }

                // 6. Tính subtotal cho item: (unit_price + toppingsExtraPrice) * quantity
                BigDecimal unitPriceWithToppings = product.getPrice().add(toppingsExtraPrice);
                BigDecimal subtotal = unitPriceWithToppings.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
                orderItem.setSubtotal(subtotal);

                order.addOrderItem(orderItem);
                totalAmount = totalAmount.add(subtotal);
            }

            order.setTotalAmount(totalAmount);

            // Lưu và flush lập tức để phát hiện lỗi Optimistic Lock ngay trong block catch
            Order savedOrder = orderRepository.saveAndFlush(order);
            return orderMapper.toDto(savedOrder);

        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException("OPTIMISTIC_LOCK_FAILURE", "Đã xảy ra xung đột khi thanh toán sản phẩm (Tồn kho vừa thay đổi). Vui lòng thử lại!");
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getUserOrders(User currentUser, Pageable pageable) {
        return orderRepository.findByUser_IdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(orderMapper::toDto);
    }

    @Transactional(readOnly = true)
    public OrderDetailDto getUserOrderDetail(User currentUser, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Đơn hàng không tồn tại"));

        // Bảo vệ Ownership
        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException("FORBIDDEN_ACCESS", "Bạn không có quyền xem thông tin đơn hàng này");
        }

        return orderMapper.toDetailDto(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> searchOrdersAdmin(String orderCode, OrderStatus status, Pageable pageable) {
        return orderRepository.searchOrdersAdmin(orderCode, status, pageable)
                .map(orderMapper::toDto);
    }

    @Transactional(readOnly = true)
    public OrderDetailDto getOrderDetailAdmin(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Đơn hàng không tồn tại"));
        return orderMapper.toDetailDto(order);
    }

    @Transactional
    public OrderDto updateOrderStatus(UUID orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Đơn hàng không tồn tại"));

        // Quy tắc chuyển trạng thái cơ bản:
        // Đã hủy/Đã hoàn thành thì không được sửa nữa
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("INVALID_STATUS_TRANSITION", "Không thể thay đổi trạng thái của đơn hàng đã Hoàn thành hoặc đã Hủy.");
        }

        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toDto(updatedOrder);
    }
}
