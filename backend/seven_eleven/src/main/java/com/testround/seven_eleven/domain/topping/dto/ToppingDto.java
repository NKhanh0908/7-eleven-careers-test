package com.testround.seven_eleven.domain.topping.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class ToppingDto {
    private UUID id;
    private String name;
    private BigDecimal extraPrice;
    private Boolean isActive;
    private Instant createdAt;
}
