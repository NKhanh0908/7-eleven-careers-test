package com.testround.seven_eleven.domain.topping;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CategoryToppingId implements Serializable {
    private UUID category;
    private UUID topping;

    public CategoryToppingId() {}

    public CategoryToppingId(UUID category, UUID topping) {
        this.category = category;
        this.topping = topping;
    }

    public UUID getCategory() {
        return category;
    }

    public void setCategory(UUID category) {
        this.category = category;
    }

    public UUID getTopping() {
        return topping;
    }

    public void setTopping(UUID topping) {
        this.topping = topping;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryToppingId that = (CategoryToppingId) o;
        return Objects.equals(category, that.category) &&
                Objects.equals(topping, that.topping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, topping);
    }
}
