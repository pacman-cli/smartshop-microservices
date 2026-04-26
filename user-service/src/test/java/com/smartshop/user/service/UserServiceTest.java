package com.smartshop.user.service;

import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
import com.smartshop.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUsersCapsPageSizeAndNormalizesPageIndex() {
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        userService.getUsers(-3, 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(100);
    }

    @Test
    void getUserByEmailNormalizesEmailBeforeLookup() {
        User user = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .password("hashed")
                .role(Role.CUSTOMER)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmailIgnoreCase("alice@example.com"))
                .thenReturn(Optional.of(user));

        var response = userService.getUserByEmail("  Alice@Example.com ");

        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        verify(userRepository).findByEmailIgnoreCase("alice@example.com");
    }
}