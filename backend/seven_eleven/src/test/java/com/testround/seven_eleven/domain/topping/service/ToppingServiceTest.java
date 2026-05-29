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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToppingServiceTest {

    @Mock
    private ToppingRepository toppingRepository;

    @Mock
    private CategoryToppingRepository categoryToppingRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ToppingMapper toppingMapper;

    @InjectMocks
    private ToppingService toppingService;

    private static final String TOPPING_NAME = "Trân châu đen";
    private static final BigDecimal EXTRA_PRICE = BigDecimal.valueOf(5000);

    private Topping buildTopping() {
        Topping topping = new Topping();
        topping.setId(UUID.randomUUID());
        topping.setName(TOPPING_NAME);
        topping.setExtraPrice(EXTRA_PRICE);
        topping.setIsActive(true);
        return topping;
    }

    private Category buildRootCategory() {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Trà sữa & Trà trái cây");
        category.setSlug("tra-sua-tra-trai-cay");
        category.setLevel((short) 0);
        category.setIsActive(true);
        return category;
    }

    private Category buildSubCategory(Category parent) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Trà sữa 7-Eleven");
        category.setSlug("tra-sua-7-eleven");
        category.setLevel((short) 1);
        category.setParent(parent);
        category.setIsActive(true);
        return category;
    }

    @Nested
    class ToppingQueries {

        @Test
        void getToppingsByCategory_whenCategoryIsRoot_returnsToppings() {
            // Arrange
            Category root = buildRootCategory();
            Topping topping = buildTopping();
            ToppingDto dto = new ToppingDto();
            dto.setId(topping.getId());
            dto.setName(topping.getName());
            dto.setExtraPrice(topping.getExtraPrice());
            dto.setIsActive(true);

            when(categoryRepository.findById(root.getId())).thenReturn(Optional.of(root));
            when(toppingRepository.findActiveToppingsByCategoryId(root.getId())).thenReturn(List.of(topping));
            when(toppingMapper.toDtoList(anyList())).thenReturn(List.of(dto));

            // Act
            List<ToppingDto> result = toppingService.getToppingsByCategory(root.getId());

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo(TOPPING_NAME);
            verify(categoryRepository).findById(root.getId());
            verify(toppingRepository).findActiveToppingsByCategoryId(root.getId());
        }

        @Test
        void getToppingsByCategory_whenCategoryIsSub_inheritsFromParent() {
            // Arrange
            Category root = buildRootCategory();
            Category sub = buildSubCategory(root);
            Topping topping = buildTopping();
            ToppingDto dto = new ToppingDto();
            dto.setId(topping.getId());
            dto.setName(topping.getName());

            when(categoryRepository.findById(sub.getId())).thenReturn(Optional.of(sub));
            when(toppingRepository.findActiveToppingsByCategoryId(root.getId())).thenReturn(List.of(topping));
            when(toppingMapper.toDtoList(anyList())).thenReturn(List.of(dto));

            // Act
            List<ToppingDto> result = toppingService.getToppingsByCategory(sub.getId());

            // Assert
            assertThat(result).hasSize(1);
            verify(categoryRepository).findById(sub.getId());
            verify(toppingRepository).findActiveToppingsByCategoryId(root.getId());
        }

        @Test
        void getToppingsByCategory_whenCategoryNotFound_throwsResourceNotFoundException() {
            // Arrange
            UUID categoryId = UUID.randomUUID();
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> toppingService.getToppingsByCategory(categoryId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void getAllToppings_returnsPaginatedResponse() {
            // Arrange
            Topping topping = buildTopping();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Topping> page = new PageImpl<>(List.of(topping));
            ToppingDto dto = new ToppingDto();
            dto.setId(topping.getId());

            when(toppingRepository.findAll(pageable)).thenReturn(page);
            when(toppingMapper.toDto(any(Topping.class))).thenReturn(dto);

            // Act
            PageResponse<ToppingDto> result = toppingService.getAllToppings(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    class ToppingCrud {

        @Test
        void createTopping_savesAndReturnsDto() {
            // Arrange
            ToppingRequest request = new ToppingRequest();
            request.setName(TOPPING_NAME);
            request.setExtraPrice(EXTRA_PRICE);

            Topping topping = buildTopping();
            ToppingDto dto = new ToppingDto();
            dto.setId(topping.getId());
            dto.setName(topping.getName());

            when(toppingMapper.toEntity(request)).thenReturn(topping);
            when(toppingRepository.save(topping)).thenReturn(topping);
            when(toppingMapper.toDto(topping)).thenReturn(dto);

            // Act
            ToppingDto result = toppingService.createTopping(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(TOPPING_NAME);
            verify(toppingRepository).save(topping);
        }

        @Test
        void updateTopping_updatesAndReturnsDto() {
            // Arrange
            UUID id = UUID.randomUUID();
            ToppingRequest request = new ToppingRequest();
            request.setName("New Name");
            request.setExtraPrice(EXTRA_PRICE);

            Topping topping = buildTopping();
            ToppingDto dto = new ToppingDto();
            dto.setId(topping.getId());
            dto.setName("New Name");

            when(toppingRepository.findById(id)).thenReturn(Optional.of(topping));
            doAnswer(invocation -> {
                Topping t = invocation.getArgument(1);
                t.setName("New Name");
                return null;
            }).when(toppingMapper).updateEntity(eq(request), any(Topping.class));
            when(toppingRepository.save(topping)).thenReturn(topping);
            when(toppingMapper.toDto(topping)).thenReturn(dto);

            // Act
            ToppingDto result = toppingService.updateTopping(id, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("New Name");
            verify(toppingRepository).save(topping);
        }

        @Test
        void deleteTopping_setsIsActiveFalse() {
            // Arrange
            UUID id = UUID.randomUUID();
            Topping topping = buildTopping();
            when(toppingRepository.findById(id)).thenReturn(Optional.of(topping));

            // Act
            toppingService.deleteTopping(id);

            // Assert
            assertThat(topping.getIsActive()).isFalse();
            verify(toppingRepository).save(topping);
        }
    }

    @Nested
    class CategoryToppingAssociation {

        @Test
        void associateToppingWithCategory_whenRootCategory_savesAssociation() {
            // Arrange
            Category root = buildRootCategory();
            Topping topping = buildTopping();
            CategoryToppingId id = new CategoryToppingId(root.getId(), topping.getId());

            when(categoryRepository.findById(root.getId())).thenReturn(Optional.of(root));
            when(toppingRepository.findById(topping.getId())).thenReturn(Optional.of(topping));
            when(categoryToppingRepository.existsById(id)).thenReturn(false);

            // Act
            toppingService.associateToppingWithCategory(root.getId(), topping.getId());

            // Assert
            verify(categoryToppingRepository).save(any(CategoryTopping.class));
        }

        @Test
        void associateToppingWithCategory_whenSubCategory_throwsBusinessException() {
            // Arrange
            Category root = buildRootCategory();
            Category sub = buildSubCategory(root);
            Topping topping = buildTopping();

            when(categoryRepository.findById(sub.getId())).thenReturn(Optional.of(sub));

            // Act & Assert
            assertThatThrownBy(() -> toppingService.associateToppingWithCategory(sub.getId(), topping.getId()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo("INVALID_CATEGORY_LEVEL");
                    });

            verify(categoryToppingRepository, never()).save(any());
        }

        @Test
        void removeToppingFromCategory_deletesAssociation() {
            // Arrange
            UUID categoryId = UUID.randomUUID();
            UUID toppingId = UUID.randomUUID();
            CategoryToppingId id = new CategoryToppingId(categoryId, toppingId);

            when(categoryToppingRepository.existsById(id)).thenReturn(true);

            // Act
            toppingService.removeToppingFromCategory(categoryId, toppingId);

            // Assert
            verify(categoryToppingRepository).deleteById(id);
        }
    }
}
