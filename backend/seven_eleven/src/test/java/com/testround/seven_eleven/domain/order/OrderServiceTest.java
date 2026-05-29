package com.testround.seven_eleven.domain.order;

import com.testround.seven_eleven.common.exception.BusinessException;
import com.testround.seven_eleven.domain.category.Category;
import com.testround.seven_eleven.domain.order.dto.OrderItemRequest;
import com.testround.seven_eleven.domain.order.dto.OrderRequest;
import com.testround.seven_eleven.domain.order.dto.OrderDto;
import com.testround.seven_eleven.domain.order.dto.OrderDetailDto;
import com.testround.seven_eleven.domain.product.Product;
import com.testround.seven_eleven.domain.product.ProductRepository;
import com.testround.seven_eleven.domain.topping.Topping;
import com.testround.seven_eleven.domain.topping.ToppingRepository;
import com.testround.seven_eleven.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ToppingRepository toppingRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Product product;
    private Category category;
    private Topping topping;
    private UUID productId;
    private UUID categoryId;
    private UUID toppingId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        toppingId = UUID.randomUUID();

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@gmail.com");

        category = new Category();
        category.setId(categoryId);
        category.setName("Trà sữa");

        product = new Product();
        product.setId(productId);
        product.setName("Trà sữa Trân châu");
        product.setPrice(new BigDecimal("30000"));
        product.setStockQuantity(10);
        product.setCategory(category);
        product.setIsActive(true);

        topping = new Topping();
        topping.setId(toppingId);
        topping.setName("Trân châu đen");
        topping.setExtraPrice(new BigDecimal("5000"));
        topping.setIsActive(true);
    }

    @Test
    void createOrder_Success() {
        // Arrange
        OrderRequest request = new OrderRequest();
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(productId);
        itemReq.setQuantity(2);
        itemReq.setToppingIds(List.of(toppingId));
        request.setItems(List.of(itemReq));
        request.setNote("Không lấy đá");

        when(orderRepository.getNextOrderSequenceValue()).thenReturn(123L);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(toppingRepository.findActiveToppingsByCategoryId(categoryId)).thenReturn(List.of(topping));

        OrderDto orderDto = new OrderDto();
        orderDto.setOrderCode("OD-20260529-0123");

        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        // Act
        OrderDto result = orderService.createOrder(user, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderCode()).contains("OD-");
        assertThat(product.getStockQuantity()).isEqualTo(8); // 10 - 2

        verify(orderRepository).getNextOrderSequenceValue();
        verify(productRepository).save(product);
        verify(orderRepository).saveAndFlush(any(Order.class));
    }

    @Test
    void createOrder_ThrowsException_WhenProductInactive() {
        // Arrange
        product.setIsActive(false);

        OrderRequest request = new OrderRequest();
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(productId);
        itemReq.setQuantity(1);
        request.setItems(List.of(itemReq));

        when(orderRepository.getNextOrderSequenceValue()).thenReturn(1L);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(user, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã ngừng kinh doanh");

        verify(productRepository, never()).save(any());
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void createOrder_ThrowsException_WhenOutOfStock() {
        // Arrange
        OrderRequest request = new OrderRequest();
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(productId);
        itemReq.setQuantity(15); // > 10 stock
        request.setItems(List.of(itemReq));

        when(orderRepository.getNextOrderSequenceValue()).thenReturn(1L);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(user, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không đủ tồn kho");

        verify(productRepository, never()).save(any());
    }

    @Test
    void createOrder_ThrowsException_WhenToppingInvalid() {
        // Arrange
        UUID wrongToppingId = UUID.randomUUID();

        OrderRequest request = new OrderRequest();
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(productId);
        itemReq.setQuantity(1);
        itemReq.setToppingIds(List.of(wrongToppingId));
        request.setItems(List.of(itemReq));

        when(orderRepository.getNextOrderSequenceValue()).thenReturn(1L);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(toppingRepository.findActiveToppingsByCategoryId(categoryId)).thenReturn(List.of(topping));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(user, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Topping không hợp lệ");

        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void createOrder_ThrowsException_OnOptimisticLockingFailure() {
        // Arrange
        OrderRequest request = new OrderRequest();
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(productId);
        itemReq.setQuantity(1);
        request.setItems(List.of(itemReq));

        when(orderRepository.getNextOrderSequenceValue()).thenReturn(1L);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(orderRepository.saveAndFlush(any(Order.class))).thenThrow(new ObjectOptimisticLockingFailureException(Order.class, UUID.randomUUID()));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(user, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Đã xảy ra xung đột khi thanh toán sản phẩm");
    }

    @Test
    void getUserOrderDetail_Success() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setUser(user);

        OrderDetailDto detailDto = new OrderDetailDto();
        detailDto.setId(orderId);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toDetailDto(order)).thenReturn(detailDto);

        // Act
        OrderDetailDto result = orderService.getUserOrderDetail(user, orderId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(orderId);
    }

    @Test
    void getUserOrderDetail_ThrowsException_WhenNotOwner() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        
        User someoneElse = new User();
        someoneElse.setId(UUID.randomUUID());

        Order order = new Order();
        order.setId(orderId);
        order.setUser(someoneElse); // owned by someone else

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> orderService.getUserOrderDetail(user, orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bạn không có quyền xem thông tin đơn hàng này");
    }
}
