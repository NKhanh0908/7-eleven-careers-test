package com.testround.seven_eleven.domain.order;

import com.testround.seven_eleven.common.BaseEntity;
import com.testround.seven_eleven.domain.topping.Topping;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item_toppings")
@Getter
@Setter
public class OrderItemTopping extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topping_id", nullable = false)
    private Topping topping;

    @Column(name = "topping_name", nullable = false, length = 100)
    private String toppingName;

    @Column(name = "extra_price", nullable = false)
    private BigDecimal extraPrice;
}
