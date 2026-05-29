package com.testround.seven_eleven.domain.order.dto;

import com.testround.seven_eleven.domain.order.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {
    @NotNull(message = "Trạng thái cập nhật không được trống")
    private OrderStatus status;
}
