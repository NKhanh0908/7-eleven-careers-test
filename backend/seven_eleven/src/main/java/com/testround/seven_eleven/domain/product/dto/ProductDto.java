package com.testround.seven_eleven.domain.product.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductDto {
    private UUID id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private Integer stockQuantity;
    private UUID categoryId;
    private String categoryName;
    private Boolean isActive;
}
