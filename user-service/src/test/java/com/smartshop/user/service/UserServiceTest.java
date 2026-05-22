package com.smartshop.user.service;

import com.smartshop.user.dto.UserResponse;
import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
import com.smartshop.user.exception.UserNotFoundException;
import com.smartshop.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    private User createSampleUser() {
        return User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .password("hashed")
                .role(Role.CUSTOMER)
                .build();
    }

    // ==================== GET USER BY ID TESTS ====================

    @Test
    void getUserByIdReturnsUser() {
        User user = createSampleUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void getUserByIdThrowsWhenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ==================== GET USER BY EMAIL TESTS ====================

    @Test
    void getUserByEmailReturnsUser() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("alice@example.com"))
                .thenReturn(Optional.of(user));

        UserResponse response = userService.getUserByEmail("alice@example.com");

        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        verify(userRepository).findByEmailIgnoreCase("alice@example.com");
    }

    @Test
    void getUserByEmailNormalizesInput() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("alice@example.com"))
                .thenReturn(Optional.of(user));

        userService.getUserByEmail("  Alice@Example.com ");

        verify(userRepository).findByEmailIgnoreCase("alice@example.com");
    }

    @Test
    void getUserByEmailThrowsWhenNotFound() {
        when(userRepository.findByEmailIgnoreCase("notfound@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("notfound@example.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ==================== GET USERS PAGINATED TESTS ====================

    @Test
    void getUsersReturnsPagedResults() {
        User user = createSampleUser();
        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<UserResponse> response = userService.getUsers(0, 10);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Alice");
    }

    @Test
    void getUsersCapsPageSizeAndNormalizesPageIndex() {
        Page<User> page = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        userService.getUsers(-3, 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void getUsersHandlesNegativePageNumber() {
        Page<User> page = new PageImpl<>(List.of());
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        userService.getUsers(-1, 10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
    }

    @Test
    void getUsersHandlesZeroSize() {
        Page<User> page = new PageImpl<>(List.of());
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        userService.getUsers(0, 0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    // ==================== GET ALL USERS (LEGACY) TESTS ====================

    @Test
    void getAllUsersReturnsFirstPage() {
        User user = createSampleUser();
        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);

        List<UserResponse> response = userService.getAllUsers();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getName()).isEqualTo("Alice");
    }

    // ==================== RESPONSE MAPPING TESTS ====================

    @Test
    void mapToResponseReturnsCorrectFields() {
        User user = createSampleUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);

        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getRole()).isEqualTo("CUSTOMER");
    }
}