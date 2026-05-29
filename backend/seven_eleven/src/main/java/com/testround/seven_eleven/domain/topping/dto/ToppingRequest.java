package com.testround.seven_eleven.domain.topping.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ToppingRequest {

    @NotBlank(message = "Tên topping không được để trống")
    @Size(max = 100, message = "Tên topping không dài quá 100 ký tự")
    private String name;

    @NotNull(message = "Giá topping không được để trống")
    @DecimalMin(value = "0.0", message = "Giá topping phải lớn hơn hoặc bằng 0")
    private BigDecimal extraPrice;

    private Boolean isActive = true;
}
