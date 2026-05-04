package com.hls.streaming.unit.user.service;

import com.hls.streaming.common.enums.ErrorEnum;
import com.hls.streaming.infrastructure.config.error.ErrorCodeMessage;
import com.hls.streaming.user.domain.document.User;
import com.hls.streaming.user.domain.enums.UserStatusEnum;
import com.hls.streaming.user.domain.repository.UserRepository;
import com.hls.streaming.user.dto.UserLiteResponse;
import com.hls.streaming.user.service.query.UserQueryService;
import com.hls.streaming.infrastructure.config.error.ErrorCodeConfig;
import com.hls.streaming.security.authentication.model.UserRole;
import com.hls.streaming.common.exception.NotFoundException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    private UserQueryService service;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ErrorCodeConfig errorCodeConfig;

    @BeforeEach
    void setUp() {
        service = new UserQueryService(userRepository, errorCodeConfig);
    }

    @Test
    void getProfile_WithValidUserId_ShouldReturnUserLiteResponse() {
        // Given
        String userId = new ObjectId().toString();

        User user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .avatar("https://example.com/avatar.jpg")
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.USER))
                .build();

        when(userRepository.findById(new ObjectId(userId))).thenReturn(Optional.of(user));

        // When
        UserLiteResponse response = service.getProfile(userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getDisplayName()).isEqualTo("Test User");
        assertThat(response.getAvatar()).isEqualTo("https://example.com/avatar.jpg");
        assertThat(response.isActive()).isTrue();
        assertThat(response.getIsAdmin()).isFalse();

        verify(userRepository).findById(new ObjectId(userId));
    }

    @Test
    void getProfile_WithAdminUser_ShouldReturnAdminFlag() {
        // Given
        String userId = new ObjectId().toString();

        User user = User.builder()
                .id(userId)
                .username("admin")
                .email("admin@example.com")
                .displayName("Admin User")
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.ADMIN))
                .build();

        when(userRepository.findById(new ObjectId(userId))).thenReturn(Optional.of(user));

        // When
        UserLiteResponse response = service.getProfile(userId);

        // Then
        assertThat(response.getIsAdmin()).isTrue();
    }

    @Test
    void getProfile_WithInactiveUser_ShouldReturnInactiveFlag() {
        // Given
        String userId = new ObjectId().toString();

        User user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .status(UserStatusEnum.INACTIVE)
                .roles(Set.of(UserRole.USER))
                .build();

        when(userRepository.findById(new ObjectId(userId))).thenReturn(Optional.of(user));

        // When
        UserLiteResponse response = service.getProfile(userId);

        // Then
        assertThat(response.isActive()).isFalse();
    }

    @Test
    void getProfile_WithNonexistentUser_ShouldThrowNotFoundException() {
        // Given
        String userId = new ObjectId().toString();

        when(userRepository.findById(new ObjectId(userId))).thenReturn(Optional.empty());
        when(errorCodeConfig.getMessage(eq(1000400021L)))
                .thenReturn(ErrorCodeMessage.builder()
                        .code(1000400021L)
                        .message("User not found")
                        .build());

        // When & Then
        assertThatThrownBy(() -> service.getProfile(userId))
                .isInstanceOf(NotFoundException.class);

        verify(userRepository).findById(new ObjectId(userId));
    }

    @Test
    void getProfile_WithMultipleRoles_ShouldHandleCorrectly() {
        // Given
        String userId = new ObjectId().toString();

        User user = User.builder()
                .id(userId)
                .username("super")
                .email("super@example.com")
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.ADMIN, UserRole.USER))
                .build();

        when(userRepository.findById(new ObjectId(userId))).thenReturn(Optional.of(user));

        // When
        UserLiteResponse response = service.getProfile(userId);

        // Then
        assertThat(response.getIsAdmin()).isTrue();
    }
}
