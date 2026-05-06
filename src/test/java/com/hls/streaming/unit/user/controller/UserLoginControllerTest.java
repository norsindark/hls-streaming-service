package com.hls.streaming.unit.user.controller;

import com.hls.streaming.common.enums.ErrorEnum;
import com.hls.streaming.infrastructure.config.error.ErrorCodeMessage;
import com.hls.streaming.user.controller.UserLoginController;
import com.hls.streaming.user.dto.RegisterUserRequest;
import com.hls.streaming.user.dto.IdentifyUserRequest;
import com.hls.streaming.user.dto.VerifyPasswordRequest;
import com.hls.streaming.user.service.command.UserRegisterService;
import com.hls.streaming.user.service.command.UserAuthService;
import com.hls.streaming.infrastructure.config.error.ErrorCodeConfig;
import com.hls.streaming.security.authentication.dto.UserAccessResponse;
import com.hls.streaming.security.context.CurrentUserProvider;
import com.hls.streaming.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLoginControllerTest {

    @Mock
    private UserRegisterService registerService;

    @Mock
    private UserAuthService authService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private UserLoginController controller;

    @BeforeEach
    void setUp() {
        // CurrentUserProvider is now injected via @InjectMocks
    }

    @Test
    void register_WithValidRequest_ShouldReturnAccessResponse() {
        RegisterUserRequest request = RegisterUserRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .displayName("Test User")
                .build();

        UserAccessResponse expectedResponse = UserAccessResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(registerService.register(request)).thenReturn(expectedResponse);

        UserAccessResponse response = controller.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");

        verify(registerService).register(request);
    }

    @Test
    void identifyUser_WithValidRequest_ShouldReturnPasswordVerificationToken() {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        UserAccessResponse expectedResponse = UserAccessResponse.builder()
                .passwordVerificationToken("verification-token")
                .build();

        when(authService.identify(request)).thenReturn(expectedResponse);

        UserAccessResponse response = controller.identifyUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getPasswordVerificationToken()).isEqualTo("verification-token");

        verify(authService).identify(request);
    }

    @Test
    void verifyPassword_WithValidToken_ShouldReturnAccessTokenPair() {
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .password("password123")
                .build();

        UserAccessResponse expectedResponse = UserAccessResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(currentUserProvider.getUserId()).thenReturn("user-id-123");
        when(authService.verifyPassword(any(VerifyPasswordRequest.class)))
                .thenReturn(expectedResponse);

        UserAccessResponse response = controller.verifyPassword(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");

        verify(authService).verifyPassword(any(VerifyPasswordRequest.class));
    }

    @Test
    void verifyPassword_WithoutToken_ShouldThrowBadRequestException() {
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .password("password123")
                .build();

        when(currentUserProvider.getUserId())
                .thenThrow(new BadRequestException(
                        ErrorCodeMessage.builder()
                                .code(403L)
                                .message("Token is missing")
                                .build()
                ));

        assertThatThrownBy(() -> controller.verifyPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Token is missing");

        verify(authService, never()).verifyPassword(any());
    }
}
