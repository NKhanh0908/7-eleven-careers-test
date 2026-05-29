package com.testround.seven_eleven.domain.product.dto;

import com.testround.seven_eleven.domain.topping.dto.ToppingDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductDetailDto extends ProductDto {
    private String description;
    private List<ToppingDto> availableToppings;
}
