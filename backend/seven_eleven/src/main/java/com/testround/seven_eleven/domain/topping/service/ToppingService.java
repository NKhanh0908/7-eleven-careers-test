package com.testround.seven_eleven.domain.topping.service;

import com.testround.seven_eleven.common.PageResponse;
import com.testround.seven_eleven.common.exception.BusinessException;
import com.testround.seven_eleven.common.exception.ResourceNotFoundException;
import com.testround.seven_eleven.domain.category.Category;
import com.testround.seven_eleven.domain.category.CategoryRepository;
import com.testround.seven_eleven.domain.topping.*;
import com.testround.seven_eleven.domain.topping.dto.ToppingDto;
import com.testround.seven_eleven.domain.topping.dto.ToppingMapper;
import com.testround.seven_eleven.domain.topping.dto.ToppingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ToppingService {

    private final ToppingRepository toppingRepository;
    private final CategoryToppingRepository categoryToppingRepository;
    private final CategoryRepository categoryRepository;
    private final ToppingMapper toppingMapper;

    @Transactional(readOnly = true)
    public List<ToppingDto> getToppingsByCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category không tồn tại"));

        // Nếu danh mục hiện tại là Sub-category (level = 1, parent != null), lấy ID của Parent (Root)
        UUID targetCategoryId = category.getParent() != null ? category.getParent().getId() : category.getId();

        List<Topping> toppings = toppingRepository.findActiveToppingsByCategoryId(targetCategoryId);
        return toppingMapper.toDtoList(toppings);
    }

    @Transactional(readOnly = true)
    public PageResponse<ToppingDto> getAllToppings(Pageable pageable) {
        Page<Topping> page = toppingRepository.findAll(pageable);
        Page<ToppingDto> dtoPage = page.map(toppingMapper::toDto);
        return PageResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public ToppingDto getToppingById(UUID id) {
        Topping topping = toppingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topping không tồn tại"));
        return toppingMapper.toDto(topping);
    }

    @Transactional
    public ToppingDto createTopping(ToppingRequest request) {
        Topping topping = toppingMapper.toEntity(request);
        topping = toppingRepository.save(topping);
        return toppingMapper.toDto(topping);
    }

    @Transactional
    public ToppingDto updateTopping(UUID id, ToppingRequest request) {
        Topping topping = toppingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topping không tồn tại"));

        toppingMapper.updateEntity(request, topping);
        topping = toppingRepository.save(topping);
        return toppingMapper.toDto(topping);
    }

    @Transactional
    public void deleteTopping(UUID id) {
        Topping topping = toppingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topping không tồn tại"));

        // Thực hiện soft delete
        topping.setIsActive(false);
        toppingRepository.save(topping);
    }

    @Transactional
    public void associateToppingWithCategory(UUID categoryId, UUID toppingId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category không tồn tại"));

        // Validate: Topping chỉ được liên kết với Root Category (level = 0)
        if (category.getLevel() != 0) {
            throw new BusinessException("INVALID_CATEGORY_LEVEL", "Topping chỉ được phép liên kết trực tiếp với Root Category (danh mục cấp gốc)");
        }

        Topping topping = toppingRepository.findById(toppingId)
                .orElseThrow(() -> new ResourceNotFoundException("Topping không tồn tại"));

        CategoryToppingId associationId = new CategoryToppingId(categoryId, toppingId);
        if (!categoryToppingRepository.existsById(associationId)) {
            CategoryTopping association = new CategoryTopping(category, topping);
            categoryToppingRepository.save(association);
        }
    }

    @Transactional
    public void removeToppingFromCategory(UUID categoryId, UUID toppingId) {
        CategoryToppingId associationId = new CategoryToppingId(categoryId, toppingId);
        if (categoryToppingRepository.existsById(associationId)) {
            categoryToppingRepository.deleteById(associationId);
        }
    }
}
