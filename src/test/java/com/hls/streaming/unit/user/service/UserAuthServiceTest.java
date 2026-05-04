package com.hls.streaming.unit.user.service;

import com.hls.streaming.user.domain.document.User;
import com.hls.streaming.user.domain.enums.UserStatusEnum;
import com.hls.streaming.user.domain.repository.UserRepository;
import com.hls.streaming.user.dto.IdentifyUserRequest;
import com.hls.streaming.user.dto.VerifyPasswordRequest;
import com.hls.streaming.user.service.command.UserAuthService;
import com.hls.streaming.user.service.internal.UserValidator;
import com.hls.streaming.security.authentication.dto.UserAccessResponse;
import com.hls.streaming.security.authentication.model.UserRole;
import com.hls.streaming.security.authentication.mapper.TokenMapper;
import com.hls.streaming.security.authentication.service.TokenFacade;
import com.hls.streaming.security.authentication.model.TokenType;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceTest {

    private UserAuthService service;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenFacade tokenFacade;

    @Mock
    private UserValidator validator;

    @Mock
    private TokenMapper tokenMapper;

    @BeforeEach
    void setUp() {
        service = new UserAuthService(userRepository, tokenFacade, validator, tokenMapper);
    }

    @Test
    void identify_WithValidEmail_ShouldReturnPasswordVerificationToken() {
        // Given
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        User user = User.builder()
                .id("user-id-123")
                .username("testuser")
                .email("test@example.com")
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.USER))
                .build();

        UserAccessResponse expectedResponse = UserAccessResponse.builder()
                .passwordVerificationToken("verification-token")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(tokenFacade.generateToken(user, TokenType.PASSWORD_VERIFICATION_TOKEN))
                .thenReturn("verification-token");
        when(tokenMapper.toLite(user)).thenReturn(null);

        // When
        UserAccessResponse response = service.identify(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPasswordVerificationToken()).isEqualTo("verification-token");

        verify(validator).validateIdentifier("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
        verify(validator).validateUserActive(user);
        verify(tokenFacade).generateToken(user, TokenType.PASSWORD_VERIFICATION_TOKEN);
    }

    @Test
    void identify_WithValidUsername_ShouldReturnPasswordVerificationToken() {
        // Given
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("testuser")
                .build();

        User user = User.builder()
                .id("user-id-123")
                .username("testuser")
                .email("test@example.com")
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.USER))
                .build();

        when(userRepository.findByEmail("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(tokenFacade.generateToken(user, TokenType.PASSWORD_VERIFICATION_TOKEN))
                .thenReturn("verification-token");

        // When
        UserAccessResponse response = service.identify(request);

        // Then
        assertThat(response).isNotNull();
        verify(userRepository).findByEmail("testuser");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void identify_WithInvalidIdentifier_ShouldThrowNotFoundException() {
        // Given
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("nonexistent")
                .build();

        when(userRepository.findByEmail("nonexistent")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.identify(request))
                .isInstanceOf(NotFoundException.class);

        verify(userRepository).findByEmail("nonexistent");
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void verifyPassword_WithValidPassword_ShouldReturnAccessTokenPair() {
        // Given
        String userId = new ObjectId().toString();
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .userId(userId)
                .password("password123")
                .build();

        User user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.USER))
                .build();

        UserAccessResponse expectedResponse = UserAccessResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(userRepository.findById(new ObjectId(userId))).thenReturn(Optional.of(user));
        when(tokenFacade.generateAccessTokenPair(user)).thenReturn(expectedResponse);

        // When
        UserAccessResponse response = service.verifyPassword(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        verify(validator).validateUserActive(user);
        verify(validator).validatePassword("password123", "encoded-password");
        verify(tokenFacade).generateAccessTokenPair(user);
    }

    @Test
    void verifyPassword_WithUserNotFound_ShouldThrowNotFoundException() {
        // Given
        String userId = new ObjectId().toString();
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .userId(userId)
                .password("password123")
                .build();

        when(userRepository.findById(new ObjectId(userId))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.verifyPassword(request))
                .isInstanceOf(NotFoundException.class);

        verify(userRepository).findById(new ObjectId(userId));
        verify(validator, never()).validateUserActive(any());
    }

    @Test
    void verifyPassword_WithInactiveUser_ShouldThrowException() {
        // Given
        String userId = new ObjectId().toString();
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .userId(userId)
                .password("password123")
                .build();

        User user = User.builder()
                .id(userId)
                .username("testuser")
                .status(UserStatusEnum.INACTIVE)
                .build();

        when(userRepository.findById(new ObjectId(userId))).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("User is inactive"))
                .when(validator).validateUserActive(user);

        // When & Then
        assertThatThrownBy(() -> service.verifyPassword(request))
                .isInstanceOf(IllegalStateException.class);

        verify(validator).validateUserActive(user);
        verify(tokenFacade, never()).generateAccessTokenPair(any());
    }
}
