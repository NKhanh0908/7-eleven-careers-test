package com.testround.seven_eleven.domain.topping.dto;

import com.testround.seven_eleven.domain.topping.Topping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ToppingMapper {
    ToppingDto toDto(Topping topping);
    Topping toEntity(ToppingRequest request);
    List<ToppingDto> toDtoList(List<Topping> toppings);
    void updateEntity(ToppingRequest request, @MappingTarget Topping topping);
}
