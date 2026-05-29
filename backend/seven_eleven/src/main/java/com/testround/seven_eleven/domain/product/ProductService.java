package com.testround.seven_eleven.domain.product;

import com.testround.seven_eleven.common.exception.BusinessException;
import com.testround.seven_eleven.domain.category.Category;
import com.testround.seven_eleven.domain.category.CategoryRepository;
import com.testround.seven_eleven.domain.product.dto.ProductDetailDto;
import com.testround.seven_eleven.domain.product.dto.ProductDto;
import com.testround.seven_eleven.domain.product.dto.ProductRequest;
import com.testround.seven_eleven.domain.product.dto.ProductStockRequest;
import com.testround.seven_eleven.domain.topping.service.ToppingService;
import com.testround.seven_eleven.domain.topping.dto.ToppingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ToppingService toppingService;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public Page<ProductDto> searchActiveProducts(UUID categoryId, String search, Pageable pageable) {
        return productRepository.searchActiveProducts(categoryId, search, pageable)
                .map(productMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> searchProductsAdmin(UUID categoryId, String search, Boolean isActive, Pageable pageable) {
        return productRepository.searchProductsAdmin(categoryId, search, isActive, pageable)
                .map(productMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ProductDetailDto getProductDetail(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Sản phẩm không tồn tại"));

        if (!product.getIsActive()) {
            throw new BusinessException("PRODUCT_INACTIVE", "Sản phẩm đã ngừng kinh doanh");
        }

        List<ToppingDto> availableToppings = toppingService.getToppingsByCategory(product.getCategory().getId());
        return productMapper.toDetailDto(product, availableToppings);
    }

    @Transactional(readOnly = true)
    public ProductDto getProductByIdAdmin(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Sản phẩm không tồn tại"));
        return productMapper.toDto(product);
    }

    @Transactional
    public ProductDto createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Danh mục không tồn tại"));

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        
        Product savedProduct = productRepository.save(product);
        return productMapper.toDto(savedProduct);
    }

    @Transactional
    public ProductDto updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Sản phẩm không tồn tại"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Danh mục không tồn tại"));

        productMapper.updateEntity(product, request);
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);
        return productMapper.toDto(updatedProduct);
    }

    @Transactional
    public ProductDto updateStock(UUID id, ProductStockRequest request) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Sản phẩm không tồn tại"));

            if (!product.getVersion().equals(request.getVersion())) {
                throw new BusinessException("OPTIMISTIC_LOCK_FAILURE", "Dữ liệu sản phẩm đã bị thay đổi bởi người khác. Vui lòng tải lại trang.");
            }

            product.setStockQuantity(request.getQuantity());
            Product updatedProduct = productRepository.save(product);
            return productMapper.toDto(updatedProduct);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException("OPTIMISTIC_LOCK_FAILURE", "Dữ liệu sản phẩm đã bị thay đổi bởi người khác. Vui lòng tải lại trang.");
        }
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Sản phẩm không tồn tại"));

        product.setIsActive(false);
        product.setDeletedAt(Instant.now());
        productRepository.save(product);
    }
}
