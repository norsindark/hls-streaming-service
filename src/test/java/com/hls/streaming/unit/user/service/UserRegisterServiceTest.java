package com.hls.streaming.unit.user.service;

import com.hls.streaming.user.domain.document.User;
import com.hls.streaming.user.domain.document.UserDetail;
import com.hls.streaming.user.domain.enums.UserStatusEnum;
import com.hls.streaming.user.domain.repository.UserRepository;
import com.hls.streaming.user.dto.RegisterUserRequest;
import com.hls.streaming.user.service.command.UserRegisterService;
import com.hls.streaming.user.service.internal.UserValidator;
import com.hls.streaming.security.authentication.dto.UserAccessResponse;
import com.hls.streaming.security.authentication.model.UserRole;
import com.hls.streaming.security.authentication.service.TokenFacade;
import com.hls.streaming.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegisterServiceTest {

    private UserRegisterService service;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenFacade tokenFacade;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserValidator validator;

    @BeforeEach
    void setUp() {
        service = new UserRegisterService(userRepository, tokenFacade, passwordEncoder, validator);
    }

    @Test
    void register_WithValidRequest_ShouldReturnAccessResponse() {
        // Given
        RegisterUserRequest request = RegisterUserRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .displayName("Test User")
                .build();

        User savedUser = User.builder()
                .id("user-id-123")
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .displayName("Test User")
                .status(UserStatusEnum.ACTIVE)
                .roles(java.util.Set.of(UserRole.USER))
                .detail(UserDetail.builder().enableNotify(true).build())
                .build();

        UserAccessResponse expectedResponse = UserAccessResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenFacade.generateAccessTokenPair(savedUser)).thenReturn(expectedResponse);

        // When
        UserAccessResponse response = service.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        verify(validator).validateRegisterInput("testuser", "test@example.com", "password123");
        verify(validator).validateUsernameNotExists("testuser");
        verify(validator).validateEmailNotExists("test@example.com");
        verify(userRepository).save(any(User.class));
        verify(tokenFacade).generateAccessTokenPair(savedUser);
    }

    @Test
    void register_WithoutDisplayName_ShouldUseUsernameAsDisplayName() {
        // Given
        RegisterUserRequest request = RegisterUserRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        User savedUser = User.builder()
                .id("user-id-123")
                .username("testuser")
                .email("test@example.com")
                .displayName("testuser")
                .status(UserStatusEnum.ACTIVE)
                .roles(java.util.Set.of(UserRole.USER))
                .build();

        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenFacade.generateAccessTokenPair(any())).thenReturn(UserAccessResponse.builder().build());

        // When
        service.register(request);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedArgument = userCaptor.getValue();
        assertThat(savedArgument.getDisplayName()).isEqualTo("testuser");
    }

    @Test
    void register_WithValidationFailure_ShouldThrowException() {
        // Given
        RegisterUserRequest request = RegisterUserRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        doThrow(new BadRequestException("Username already exists"))
                .when(validator).validateUsernameNotExists("testuser");

        // When & Then
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ShouldSaveWithUserRoleAndActiveStatus() {
        // Given
        RegisterUserRequest request = RegisterUserRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        User savedUser = User.builder()
                .id("user-id-123")
                .username("testuser")
                .email("test@example.com")
                .status(UserStatusEnum.ACTIVE)
                .roles(java.util.Set.of(UserRole.USER))
                .build();

        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenFacade.generateAccessTokenPair(any())).thenReturn(UserAccessResponse.builder().build());

        // When
        service.register(request);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedArgument = userCaptor.getValue();
        assertThat(savedArgument.getStatus()).isEqualTo(UserStatusEnum.ACTIVE);
        assertThat(savedArgument.getRoles()).containsExactly(UserRole.USER);
    }
}
