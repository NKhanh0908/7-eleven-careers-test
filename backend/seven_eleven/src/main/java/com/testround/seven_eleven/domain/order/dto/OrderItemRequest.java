package com.testround.seven_eleven.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class OrderItemRequest {
    @NotNull(message = "Product ID không được để trống")
    private UUID productId;

    @NotNull(message = "Số lượng sản phẩm không được để trống")
    @Min(value = 1, message = "Số lượng sản phẩm phải tối thiểu là 1")
    private Integer quantity;

    private List<UUID> toppingIds = new ArrayList<>();
}
