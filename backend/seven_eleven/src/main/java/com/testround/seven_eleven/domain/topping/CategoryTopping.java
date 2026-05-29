package com.testround.seven_eleven.domain.topping;

import com.testround.seven_eleven.domain.category.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "category_toppings")
@IdClass(CategoryToppingId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTopping {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topping_id")
    private Topping topping;
}
