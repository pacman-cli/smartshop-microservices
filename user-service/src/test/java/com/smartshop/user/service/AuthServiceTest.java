package com.smartshop.user.service;

import com.smartshop.user.dto.AuthResponse;
import com.smartshop.user.dto.LoginRequest;
import com.smartshop.user.dto.RegisterRequest;
import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
import com.smartshop.user.exception.UserAlreadyExistsException;
import com.smartshop.user.exception.UserNotFoundException;
import com.smartshop.user.repository.UserRepository;
import com.smartshop.user.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil);
    }

    // ==================== REGISTER TESTS ====================

    @Test
    void registerSuccessfullyCreatesUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@test.com");
        request.setPassword("password123");

        User savedUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@test.com")
                .password("hashedPassword")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.existsByEmailIgnoreCase("john@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyString(), anyString(), any())).thenReturn("jwt-token-xyz");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-xyz");
        assertThat(response.getEmail()).isEqualTo("john@test.com");
        assertThat(response.getName()).isEqualTo("John Doe");
        assertThat(response.getRole()).isEqualTo("CUSTOMER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("existing@test.com");
        request.setPassword("password123");

        when(userRepository.existsByEmailIgnoreCase("existing@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("already registered");

        verify(userRepository).existsByEmailIgnoreCase("existing@test.com");
    }

    @Test
    void registerNormalizesEmailToLowercase() {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("JOHN@TEST.COM");
        request.setPassword("password123");

        User savedUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@test.com")
                .password("hashed")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.existsByEmailIgnoreCase("john@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyString(), anyString(), any())).thenReturn("token");

        authService.register(request);

        var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("john@test.com");
    }

    @Test
    void registerTrimsNameAndEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setName("  John Doe  ");
        request.setEmail("  john@test.com  ");
        request.setPassword("password123");

        User savedUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@test.com")
                .password("hashed")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.existsByEmailIgnoreCase("john@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyString(), anyString(), any())).thenReturn("token");

        authService.register(request);

        var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getName()).isEqualTo("John Doe");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("john@test.com");
    }

    @Test
    void registerAssignsCustomerRoleByDefault() {
        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("new@test.com");
        request.setPassword("password123");

        User savedUser = User.builder()
                .id(1L)
                .name("New User")
                .email("new@test.com")
                .password("hashed")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyString(), anyString(), any())).thenReturn("token");

        authService.register(request);

        var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.CUSTOMER);
    }

    // ==================== LOGIN TESTS ====================

    @Test
    void loginSuccessfullyReturnsToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@test.com");
        request.setPassword("password123");

        User user = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@test.com")
                .password("hashedPassword")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.findByEmailIgnoreCase("john@test.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString(), any())).thenReturn("jwt-token-abc");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-abc");
        assertThat(response.getEmail()).isEqualTo("john@test.com");
        assertThat(response.getRole()).isEqualTo("CUSTOMER");
    }

    @Test
    void loginThrowsWhenUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("notfound@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmailIgnoreCase("notfound@test.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void loginThrowsWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@test.com");
        request.setPassword("wrongPassword");

        User user = User.builder()
                .id(1L)
                .email("john@test.com")
                .password("hashedPassword")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.findByEmailIgnoreCase("john@test.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void loginNormalizesEmailToLowercase() {
        LoginRequest request = new LoginRequest();
        request.setEmail("JOHN@TEST.COM");
        request.setPassword("password123");

        User user = User.builder()
                .id(1L)
                .email("john@test.com")
                .password("hashedPassword")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.findByEmailIgnoreCase("john@test.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString(), any())).thenReturn("token");

        authService.login(request);

        verify(userRepository).findByEmailIgnoreCase("john@test.com");
    }

    @Test
    void loginGeneratesTokenWithCorrectClaims() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("password123");

        User user = User.builder()
                .id(5L)
                .name("Admin User")
                .email("admin@test.com")
                .password("hashedPassword")
                .role(Role.ADMIN)
                .build();

        when(userRepository.findByEmailIgnoreCase("admin@test.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        authService.login(request);

        verify(jwtUtil).generateToken(eq("admin@test.com"), eq("ADMIN"), eq(5L));
    }
}