package com.testround.seven_eleven.domain.order;

import com.testround.seven_eleven.common.ApiResponse;
import com.testround.seven_eleven.domain.order.dto.OrderDetailDto;
import com.testround.seven_eleven.domain.order.dto.OrderDto;
import com.testround.seven_eleven.domain.order.dto.OrderRequest;
import com.testround.seven_eleven.domain.user.User;
import com.testround.seven_eleven.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Order API", description = "API quản lý đặt hàng dành cho người dùng")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Tạo đơn hàng mới", description = "Khách đặt hàng, giảm tồn kho sản phẩm, áp dụng Optimistic Lock và Snapshot dữ liệu")
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            @CurrentUser User currentUser,
            @Valid @RequestBody OrderRequest request) {

        OrderDto orderDto = orderService.createOrder(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(orderDto, "Đặt hàng thành công", HttpStatus.CREATED.value()));
    }

    @GetMapping
    @Operation(summary = "Xem lịch sử đặt hàng cá nhân", description = "Phân trang lịch sử đơn hàng của User hiện tại")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getUserOrders(
            @CurrentUser User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDto> orders = orderService.getUserOrders(currentUser, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết 1 đơn hàng", description = "Xem chi tiết các mặt hàng và topping đã đặt (Có kiểm tra quyền sở hữu)")
    public ResponseEntity<ApiResponse<OrderDetailDto>> getUserOrderDetail(
            @CurrentUser User currentUser,
            @PathVariable UUID id) {

        OrderDetailDto detail = orderService.getUserOrderDetail(currentUser, id);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }
}
