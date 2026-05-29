package com.testround.seven_eleven.domain.order;

import com.testround.seven_eleven.common.ApiResponse;
import com.testround.seven_eleven.domain.order.dto.OrderDetailDto;
import com.testround.seven_eleven.domain.order.dto.OrderDto;
import com.testround.seven_eleven.domain.order.dto.OrderStatusUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Order API", description = "API quản lý đơn hàng dành cho Admin")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Lấy danh sách đơn hàng (Admin)", description = "Hỗ trợ lọc theo mã đơn hàng hoặc trạng thái, phân trang")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> searchOrders(
            @RequestParam(required = false) String orderCode,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OrderDto> orders = orderService.searchOrdersAdmin(orderCode, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết đơn hàng (Admin)")
    public ResponseEntity<ApiResponse<OrderDetailDto>> getOrderDetailAdmin(@PathVariable UUID id) {
        OrderDetailDto detail = orderService.getOrderDetailAdmin(id);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái đơn hàng (Admin)", description = "Chuyển đổi các trạng thái: PENDING -> CONFIRMED -> COMPLETED / CANCELLED")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {

        OrderDto orderDto = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(orderDto, "Cập nhật trạng thái đơn hàng thành công", HttpStatus.OK.value()));
    }
}
