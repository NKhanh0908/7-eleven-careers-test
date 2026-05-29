package com.testround.seven_eleven.domain.topping.controller;

import com.testround.seven_eleven.common.ApiResponse;
import com.testround.seven_eleven.common.PageResponse;
import com.testround.seven_eleven.domain.topping.dto.ToppingDto;
import com.testround.seven_eleven.domain.topping.dto.ToppingRequest;
import com.testround.seven_eleven.domain.topping.service.ToppingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ToppingController {

    private final ToppingService toppingService;

    // Public API: Lấy danh sách topping khả dụng theo category ID (chỉ gán ở Root Category, Sub-category tự kế thừa)
    @GetMapping("/api/v1/categories/{categoryId}/toppings")
    public ResponseEntity<ApiResponse<List<ToppingDto>>> getToppingsByCategory(@PathVariable UUID categoryId) {
        List<ToppingDto> toppings = toppingService.getToppingsByCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success(toppings));
    }

    // Admin API: Lấy danh sách phân trang tất cả toppings
    @GetMapping("/api/v1/admin/toppings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ToppingDto>>> getAllToppings(Pageable pageable) {
        PageResponse<ToppingDto> toppings = toppingService.getAllToppings(pageable);
        return ResponseEntity.ok(ApiResponse.success(toppings));
    }

    // Admin API: Lấy chi tiết topping theo ID
    @GetMapping("/api/v1/admin/toppings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ToppingDto>> getToppingById(@PathVariable UUID id) {
        ToppingDto topping = toppingService.getToppingById(id);
        return ResponseEntity.ok(ApiResponse.success(topping));
    }

    // Admin API: Tạo mới topping
    @PostMapping("/api/v1/admin/toppings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ToppingDto>> createTopping(@Valid @RequestBody ToppingRequest request) {
        ToppingDto created = toppingService.createTopping(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Tạo topping thành công", 201));
    }

    // Admin API: Cập nhật thông tin topping
    @PutMapping("/api/v1/admin/toppings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ToppingDto>> updateTopping(
            @PathVariable UUID id,
            @Valid @RequestBody ToppingRequest request) {
        ToppingDto updated = toppingService.updateTopping(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật topping thành công", 200));
    }

    // Admin API: Soft delete topping (isActive = false)
    @DeleteMapping("/api/v1/admin/toppings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTopping(@PathVariable UUID id) {
        toppingService.deleteTopping(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa topping thành công", 200));
    }

    // Admin API: Liên kết topping vào root category
    @PostMapping("/api/v1/admin/categories/{categoryId}/toppings/{toppingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> associateTopping(
            @PathVariable UUID categoryId,
            @PathVariable UUID toppingId) {
        toppingService.associateToppingWithCategory(categoryId, toppingId);
        return ResponseEntity.ok(ApiResponse.success(null, "Liên kết topping với danh mục thành công", 200));
    }

    // Admin API: Gỡ liên kết topping khỏi category
    @DeleteMapping("/api/v1/admin/categories/{categoryId}/toppings/{toppingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeAssociation(
            @PathVariable UUID categoryId,
            @PathVariable UUID toppingId) {
        toppingService.removeToppingFromCategory(categoryId, toppingId);
        return ResponseEntity.ok(ApiResponse.success(null, "Gỡ liên kết topping khỏi danh mục thành công", 200));
    }
}
