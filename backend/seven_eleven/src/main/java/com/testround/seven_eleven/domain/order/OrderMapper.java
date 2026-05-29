package com.testround.seven_eleven.domain.order;

import com.testround.seven_eleven.domain.order.dto.OrderDetailDto;
import com.testround.seven_eleven.domain.order.dto.OrderDto;
import com.testround.seven_eleven.domain.order.dto.OrderItemDto;
import com.testround.seven_eleven.domain.order.dto.OrderItemToppingDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderDto toDto(Order order);

    OrderDetailDto toDetailDto(Order order);

    @Mapping(target = "productId", source = "product.id")
    OrderItemDto toOrderItemDto(OrderItem orderItem);

    @Mapping(target = "toppingId", source = "topping.id")
    OrderItemToppingDto toOrderItemToppingDto(OrderItemTopping orderItemTopping);
}
