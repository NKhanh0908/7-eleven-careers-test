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
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthService authService;

    private static final String EMAIL = "test@7eleven.vn";
    private static final String PASSWORD = "Password@123";
    private static final String FULL_NAME = "Test User";

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "cookieName", "access_token");
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "cookieSecure", false);
    }

    private User buildActiveUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(EMAIL);
        user.setPasswordHash("hashed_password");
        user.setFullName(FULL_NAME);
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        return user;
    }

    private User buildInactiveUser() {
        User user = buildActiveUser();
        user.setIsActive(false);
        return user;
    }

    @Nested
    class Login {

        @Test
        void login_whenValidCredentials_returnsAuthResponse() {
            // Arrange
            User user = buildActiveUser();
            LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
            Authentication authentication = mock(Authentication.class);

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtTokenProvider.generateToken(authentication)).thenReturn("jwt_token");

            // Act
            AuthResponse result = authService.login(request, response);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUser().email()).isEqualTo(EMAIL);
            verify(response, times(1)).addHeader(eq("Set-Cookie"), anyString());
        }

        @Test
        void login_whenUserNotFound_throwsBusinessException() {
            // Arrange
            LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.login(request, response))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo("UNAUTHORIZED");
                    });
        }

        @Test
        void login_whenUserInactive_throwsBusinessException() {
            // Arrange
            User user = buildInactiveUser();
            LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(request, response))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo("UNAUTHORIZED");
                        assertThat(be.getMessage()).contains("khóa");
                    });
        }

        @Test
        void login_whenWrongPassword_throwsBusinessException() {
            // Arrange
            User user = buildActiveUser();
            LoginRequest request = new LoginRequest(EMAIL, "WrongPassword");
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new RuntimeException("Bad credentials"));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(request, response))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo("UNAUTHORIZED");
                    });
        }
    }

    @Nested
    class Register {

        @Test
        void register_whenEmailAlreadyExists_throwsBusinessException() {
            // Arrange
            RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, FULL_NAME);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
                    });
        }

        @Test
        void register_whenValidRequest_encodesPasswordBeforeSave() {
            // Arrange
            RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, FULL_NAME);
            User savedUser = buildActiveUser();
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // Act
            UserDto result = authService.register(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.email()).isEqualTo(EMAIL);
            verify(passwordEncoder, times(1)).encode(PASSWORD);
            verify(userRepository, times(1)).save(any(User.class));
        }
    }
}
