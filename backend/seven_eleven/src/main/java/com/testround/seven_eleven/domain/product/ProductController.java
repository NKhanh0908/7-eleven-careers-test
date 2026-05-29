package com.testround.seven_eleven.domain.product;

import com.testround.seven_eleven.common.ApiResponse;
import com.testround.seven_eleven.domain.product.dto.ProductDetailDto;
import com.testround.seven_eleven.domain.product.dto.ProductDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "User Product API", description = "API quản lý sản phẩm dành cho người dùng cuối")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Tìm kiếm và lọc sản phẩm", description = "Tìm sản phẩm theo category hoặc từ khóa, hỗ trợ phân trang (chỉ hiển thị sản phẩm đang kinh doanh)")
    public ResponseEntity<ApiResponse<Page<ProductDto>>> searchProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductDto> products = productService.searchActiveProducts(categoryId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết sản phẩm", description = "Lấy chi tiết sản phẩm và danh sách topping khả dụng cho sản phẩm đó")
    public ResponseEntity<ApiResponse<ProductDetailDto>> getProductDetail(@PathVariable UUID id) {
        ProductDetailDto detail = productService.getProductDetail(id);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }
}
