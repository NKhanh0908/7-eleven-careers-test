package com.testround.seven_eleven.domain.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    
    // Lấy các danh mục gốc (level = 0 hoặc parent is null)
    List<Category> findByParentIsNullOrderBySortOrderAsc();

    // Lấy toàn bộ danh mục đang active để load tree lên cache
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.isActive = true ORDER BY c.sortOrder ASC")
    List<Category> findAllActiveCategoriesWithChildren();
}
