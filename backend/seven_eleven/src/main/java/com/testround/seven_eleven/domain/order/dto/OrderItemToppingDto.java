package com.testround.seven_eleven.domain.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderItemToppingDto {
    private UUID id;
    private UUID toppingId;
    private String toppingName;
    private BigDecimal extraPrice;
}
