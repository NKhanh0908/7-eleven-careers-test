package com.testround.seven_eleven.domain.product.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductStockRequest {
    @NotNull(message = "Số lượng tồn kho không được để trống")
    private Integer quantity;

    @NotNull(message = "Phiên bản (version) hiện tại không được để trống để đảm bảo toàn vẹn dữ liệu")
    private Long version;
}
