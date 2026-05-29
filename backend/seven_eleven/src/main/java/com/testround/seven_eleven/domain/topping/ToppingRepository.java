package com.testround.seven_eleven.domain.topping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ToppingRepository extends JpaRepository<Topping, UUID> {

    @Query("""
        SELECT t FROM Topping t
        JOIN CategoryTopping ct ON ct.topping.id = t.id
        WHERE ct.category.id = :categoryId
          AND t.isActive = true
        """)
    List<Topping> findActiveToppingsByCategoryId(@Param("categoryId") UUID categoryId);
}
