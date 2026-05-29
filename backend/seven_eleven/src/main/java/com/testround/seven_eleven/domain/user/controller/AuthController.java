package com.testround.seven_eleven.domain.user.controller;

import com.testround.seven_eleven.common.ApiResponse;
import com.testround.seven_eleven.domain.user.dto.AuthResponse;
import com.testround.seven_eleven.domain.user.dto.LoginRequest;
import com.testround.seven_eleven.domain.user.dto.RegisterRequest;
import com.testround.seven_eleven.domain.user.dto.UserDto;
import com.testround.seven_eleven.domain.user.service.AuthService;
import com.testround.seven_eleven.security.CurrentUser;
import com.testround.seven_eleven.security.RateLimit;
import com.testround.seven_eleven.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @RateLimit(requests = 10, perSeconds = 60)
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Đăng nhập thành công", 200));
    }

    @PostMapping("/register")
    @RateLimit(requests = 5, perSeconds = 60)
    public ResponseEntity<ApiResponse<UserDto>> register(
            @Valid @RequestBody RegisterRequest request) {
        UserDto userDto = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userDto, "Đăng ký tài khoản thành công", 201));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công", 200));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDetailsImpl>> getMe(@CurrentUser UserDetailsImpl currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "Chưa đăng nhập", 401));
        }
        return ResponseEntity.ok(ApiResponse.success(currentUser));
    }
}
