package com.testround.seven_eleven.domain.user.service;

import com.testround.seven_eleven.common.exception.BusinessException;
import com.testround.seven_eleven.domain.user.User;
import com.testround.seven_eleven.domain.user.UserRepository;
import com.testround.seven_eleven.domain.user.UserRole;
import com.testround.seven_eleven.domain.user.dto.AuthResponse;
import com.testround.seven_eleven.domain.user.dto.LoginRequest;
import com.testround.seven_eleven.domain.user.dto.RegisterRequest;
import com.testround.seven_eleven.domain.user.dto.UserDto;
import com.testround.seven_eleven.security.JwtTokenProvider;
import com.testround.seven_eleven.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.cookie-name}")
    private String cookieName;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        // Load user first to check if they are active
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", "Email hoặc mật khẩu không chính xác"));

        if (!user.getIsActive()) {
            throw new BusinessException("UNAUTHORIZED", "Tài khoản của bạn đã bị khóa");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenProvider.generateToken(authentication);

            ResponseCookie cookie = ResponseCookie.from(cookieName, jwt)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(Duration.ofDays(7))
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            return AuthResponse.builder()
                    .user(UserDto.from(user))
                    .build();
        } catch (Exception e) {
            throw new BusinessException("UNAUTHORIZED", "Email hoặc mật khẩu không chính xác");
        }
    }

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "Email đã được sử dụng");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(UserRole.USER)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        return UserDto.from(user);
    }

    public void logout(HttpServletResponse response) {
        ResponseCookie deleteCookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
    }
}
