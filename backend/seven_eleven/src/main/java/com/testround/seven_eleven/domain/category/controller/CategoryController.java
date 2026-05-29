package com.testround.seven_eleven.domain.category.controller;

import com.testround.seven_eleven.common.ApiResponse;
import com.testround.seven_eleven.domain.category.dto.CategoryDto;
import com.testround.seven_eleven.domain.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryDto>> getCategoryTree() {
        return ApiResponse.success(categoryService.getCategoryTree());
    }
}
