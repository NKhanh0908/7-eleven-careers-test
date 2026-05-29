package com.testround.seven_eleven.domain.topping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategoryToppingRepository extends JpaRepository<CategoryTopping, CategoryToppingId> {
    void deleteByCategoryIdAndToppingId(UUID categoryId, UUID toppingId);
    boolean existsByCategoryIdAndToppingId(UUID categoryId, UUID toppingId);
}
