package com.testround.seven_eleven.domain.product;

import com.testround.seven_eleven.common.exception.BusinessException;
import com.testround.seven_eleven.domain.category.Category;
import com.testround.seven_eleven.domain.category.CategoryRepository;
import com.testround.seven_eleven.domain.product.dto.ProductDetailDto;
import com.testround.seven_eleven.domain.product.dto.ProductStockRequest;
import com.testround.seven_eleven.domain.topping.service.ToppingService;
import com.testround.seven_eleven.domain.topping.dto.ToppingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ToppingService toppingService;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private Category category;
    private UUID productId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = new Category();
        category.setId(categoryId);
        category.setName("Trà sữa");

        product = new Product();
        product.setId(productId);
        product.setName("Trà sữa Trân châu đen");
        product.setPrice(new BigDecimal("35000"));
        product.setStockQuantity(100);
        product.setVersion(1L);
        product.setCategory(category);
        product.setIsActive(true);
    }

    @Test
    void getProductDetail_Success() {
        // Arrange
        ToppingDto toppingDto = new ToppingDto();
        toppingDto.setName("Trân châu đen");
        List<ToppingDto> toppings = List.of(toppingDto);

        ProductDetailDto detailDto = new ProductDetailDto();
        detailDto.setId(productId);
        detailDto.setAvailableToppings(toppings);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(toppingService.getToppingsByCategory(categoryId)).thenReturn(toppings);
        when(productMapper.toDetailDto(product, toppings)).thenReturn(detailDto);

        // Act
        ProductDetailDto result = productService.getProductDetail(productId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAvailableToppings()).hasSize(1);
        verify(productRepository).findById(productId);
        verify(toppingService).getToppingsByCategory(categoryId);
    }

    @Test
    void getProductDetail_ThrowsException_WhenProductInactive() {
        // Arrange
        product.setIsActive(false);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() -> productService.getProductDetail(productId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Sản phẩm đã ngừng kinh doanh");

        verify(toppingService, never()).getToppingsByCategory(any());
    }

    @Test
    void updateStock_Success() {
        // Arrange
        ProductStockRequest request = new ProductStockRequest();
        request.setQuantity(50);
        request.setVersion(1L); // Match current version

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        productService.updateStock(productId, request);

        // Assert
        assertThat(product.getStockQuantity()).isEqualTo(50);
        verify(productRepository).save(product);
    }

    @Test
    void updateStock_ThrowsException_WhenVersionMismatch() {
        // Arrange
        ProductStockRequest request = new ProductStockRequest();
        request.setQuantity(50);
        request.setVersion(2L); // Mismatch version

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() -> productService.updateStock(productId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Dữ liệu sản phẩm đã bị thay đổi");

        verify(productRepository, never()).save(any());
    }

    @Test
    void updateStock_ThrowsException_OnOptimisticLockingFailure() {
        // Arrange
        ProductStockRequest request = new ProductStockRequest();
        request.setQuantity(50);
        request.setVersion(1L); // Match initially

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenThrow(new ObjectOptimisticLockingFailureException(Product.class, productId));

        // Act & Assert
        assertThatThrownBy(() -> productService.updateStock(productId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Dữ liệu sản phẩm đã bị thay đổi");
    }

    @Test
    void deleteProduct_Success_SoftDelete() {
        // Arrange
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // Act
        productService.deleteProduct(productId);

        // Assert
        assertThat(product.getIsActive()).isFalse();
        assertThat(product.getDeletedAt()).isNotNull();
        verify(productRepository).save(product);
    }
}
