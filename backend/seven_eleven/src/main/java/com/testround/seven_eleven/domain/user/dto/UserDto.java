package com.testround.seven_eleven.domain.user.dto;

import com.testround.seven_eleven.domain.user.User;
import com.testround.seven_eleven.domain.user.UserRole;

import java.util.UUID;

public record UserDto(
    UUID id,
    String email,
    String fullName,
    UserRole role
) {
    public static UserDto from(User user) {
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole()
        );
    }
}
