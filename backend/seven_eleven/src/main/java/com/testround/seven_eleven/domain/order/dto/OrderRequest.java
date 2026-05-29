package com.testround.seven_eleven.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrderRequest {
    @NotEmpty(message = "Danh sách sản phẩm đặt hàng không được trống")
    @Valid
    private List<OrderItemRequest> items = new ArrayList<>();

    private String note;
}
