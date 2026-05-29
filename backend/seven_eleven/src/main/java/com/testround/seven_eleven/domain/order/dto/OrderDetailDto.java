package com.testround.seven_eleven.domain.order.dto;

import com.testround.seven_eleven.domain.order.OrderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailDto extends OrderDto {
    private String note;
    private List<OrderItemDto> orderItems;
}
