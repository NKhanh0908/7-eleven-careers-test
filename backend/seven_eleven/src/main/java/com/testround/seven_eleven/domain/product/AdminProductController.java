package com.testround.seven_eleven.domain.product;

import com.testround.seven_eleven.common.ApiResponse;
import com.testround.seven_eleven.domain.product.dto.ProductDto;
import com.testround.seven_eleven.domain.product.dto.ProductRequest;
import com.testround.seven_eleven.domain.product.dto.ProductStockRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.testround.seven_eleven.common.service.CloudinaryService;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Product API", description = "API quản lý sản phẩm dành cho Admin")
public class AdminProductController {

    private final ProductService productService;
    private final CloudinaryService cloudinaryService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả sản phẩm (Admin)", description = "Hỗ trợ lọc theo trạng thái isActive, phân trang, sắp xếp")
    public ResponseEntity<ApiResponse<Page<ProductDto>>> searchProductsAdmin(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductDto> products = productService.searchProductsAdmin(categoryId, search, isActive, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết 1 sản phẩm (Admin)")
    public ResponseEntity<ApiResponse<ProductDto>> getProductByIdAdmin(@PathVariable UUID id) {
        ProductDto product = productService.getProductByIdAdmin(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PostMapping
    @Operation(summary = "Thêm mới sản phẩm")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductDto product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(product, "Tạo sản phẩm thành công", HttpStatus.CREATED.value()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật sản phẩm")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request) {
        ProductDto product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(product, "Cập nhật sản phẩm thành công", HttpStatus.OK.value()));
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Cập nhật số lượng tồn kho (Hỗ trợ Optimistic Locking)")
    public ResponseEntity<ApiResponse<ProductDto>> updateStock(
            @PathVariable UUID id,
            @Valid @RequestBody ProductStockRequest request) {
        ProductDto product = productService.updateStock(id, request);
        return ResponseEntity.ok(ApiResponse.success(product, "Cập nhật tồn kho thành công", HttpStatus.OK.value()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa mềm sản phẩm (Ngừng kinh doanh)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa sản phẩm thành công", HttpStatus.OK.value()));
    }

    @PostMapping(value = "/upload-image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải ảnh sản phẩm lên Cloudinary", description = "Trả về URL của ảnh sau khi tải lên thành công")
    public ResponseEntity<ApiResponse<String>> uploadProductImage(
            @RequestParam("file") MultipartFile file) {
        String imageUrl = cloudinaryService.uploadImage(file);
        return ResponseEntity.ok(ApiResponse.success(imageUrl, "Tải ảnh lên Cloudinary thành công", HttpStatus.OK.value()));
    }
}
