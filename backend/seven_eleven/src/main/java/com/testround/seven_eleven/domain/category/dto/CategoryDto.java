package com.testround.seven_eleven.domain.category.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class CategoryDto {
    private UUID id;
    private String name;
    private String slug;
    private Short level;
    private Short sortOrder;
    private List<CategoryDto> children;
}
