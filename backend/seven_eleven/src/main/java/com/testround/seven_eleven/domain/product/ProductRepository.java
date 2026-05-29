package com.testround.seven_eleven.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("""
        SELECT p FROM Product p
        WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
          AND p.isActive = true
        """)
    Page<Product> searchActiveProducts(
            @Param("categoryId") UUID categoryId,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:isActive IS NULL OR p.isActive = :isActive)
        """)
    Page<Product> searchProductsAdmin(
            @Param("categoryId") UUID categoryId,
            @Param("search") String search,
            @Param("isActive") Boolean isActive,
            Pageable pageable);
}
