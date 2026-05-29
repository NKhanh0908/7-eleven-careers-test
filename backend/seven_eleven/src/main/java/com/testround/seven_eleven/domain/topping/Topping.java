package com.testround.seven_eleven.domain.topping;

import com.testround.seven_eleven.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "toppings")
@Getter
@Setter
public class Topping extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, name = "extra_price")
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @Column(nullable = false, name = "is_active")
    private Boolean isActive = true;
}
