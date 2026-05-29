package com.testround.seven_eleven.domain.category.service;

import com.testround.seven_eleven.domain.category.Category;
import com.testround.seven_eleven.domain.category.CategoryRepository;
import com.testround.seven_eleven.domain.category.dto.CategoryDto;
import com.testround.seven_eleven.domain.category.dto.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategoryTree() {
        // Lấy danh sách các root category (level = 0 hoặc parent = null)
        // Vì sử dụng LAZY loading, ta dùng truy vấn cơ bản lấy root.
        // Tuy nhiên, để tối ưu truy vấn đệ quy N+1, ta có thể dùng findAllActiveCategoriesWithChildren
        // Dưới đây dùng cách lấy root, và mapper sẽ map children do cascade/LAZY (coi chừng N+1 nếu không fetch join).
        // Fetch join đã được cấu hình trong repository (đối với query lấy all).
        // Đối với tree, ta thường load những cây root ra.
        
        List<Category> rootCategories = categoryRepository.findByParentIsNullOrderBySortOrderAsc();
        return categoryMapper.toDtoList(rootCategories);
    }
}
