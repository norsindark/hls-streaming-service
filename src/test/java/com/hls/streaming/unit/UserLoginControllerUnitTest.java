package com.hls.streaming.unit;

import com.hls.streaming.controllers.user.UserLoginController;
import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.dtos.user.IdentifyUserRequest;
import com.hls.streaming.dtos.user.VerifyPasswordRequest;
import com.hls.streaming.enums.UserFlowStatusEnum;
import com.hls.streaming.services.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserLoginController Unit Tests")
class UserLoginControllerUnitTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserLoginController controller;

    private IdentifyUserRequest identifyUserRequest;
    private VerifyPasswordRequest verifyPasswordRequest;
    private UserAccessResponse mockResponse;

    @BeforeEach
    void setUp() {
        identifyUserRequest = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        verifyPasswordRequest = VerifyPasswordRequest.builder()
                .userId("user-123")
                .password("password123")
                .build();

        mockResponse = UserAccessResponse.builder()
                .status(UserFlowStatusEnum.COMPLETED)
                .accessToken("token123")
                .build();
    }

    @Nested
    @DisplayName("identifyUser Tests")
    class IdentifyUserTests {

        @Test
        @DisplayName("Should delegate to UserService.identifyUser")
        void shouldDelegateIdentifyUserToService() {
            when(userService.identifyUser(identifyUserRequest))
                    .thenReturn(mockResponse);

            UserAccessResponse response = controller.identifyUser(identifyUserRequest);

            assertNotNull(response);
            assertEquals(mockResponse, response);
            verify(userService, times(1)).identifyUser(identifyUserRequest);
        }

        @Test
        @DisplayName("Should return response from UserService")
        void shouldReturnServiceResponse() {
            UserAccessResponse expectedResponse = UserAccessResponse.builder()
                    .status(UserFlowStatusEnum.WAITING_FOR_VERIFICATION)
                    .passwordVerificationToken("tempToken")
                    .build();

            when(userService.identifyUser(any(IdentifyUserRequest.class)))
                    .thenReturn(expectedResponse);

            UserAccessResponse response = controller.identifyUser(identifyUserRequest);

            assertEquals(expectedResponse.getStatus(), response.getStatus());
            assertEquals(expectedResponse.getPasswordVerificationToken(), response.getPasswordVerificationToken());
        }

        @Test
        @DisplayName("Should throw exception when UserService throws")
        void shouldPropagateException() {
            when(userService.identifyUser(any(IdentifyUserRequest.class)))
                    .thenThrow(new RuntimeException("Service error"));

            assertThrows(RuntimeException.class,
                    () -> controller.identifyUser(identifyUserRequest));
        }
    }

    @Nested
    @DisplayName("verifyPassword Tests")
    class VerifyPasswordTests {

        @Test
        @DisplayName("Should delegate to UserService.verifyPassword")
        void shouldDelegateVerifyPasswordToService() {
            when(userService.verifyPassword(verifyPasswordRequest))
                    .thenReturn(mockResponse);

            UserAccessResponse response = controller.verifyPassword(verifyPasswordRequest);

            assertNotNull(response);
            assertEquals(mockResponse, response);
            verify(userService, times(1)).verifyPassword(verifyPasswordRequest);
        }

        @Test
        @DisplayName("Should return tokens from UserService")
        void shouldReturnTokens() {
            UserAccessResponse expectedResponse = UserAccessResponse.builder()
                    .status(UserFlowStatusEnum.COMPLETED)
                    .accessToken("accessToken123")
                    .refreshToken("refreshToken456")
                    .build();

            when(userService.verifyPassword(any(VerifyPasswordRequest.class)))
                    .thenReturn(expectedResponse);

            UserAccessResponse response = controller.verifyPassword(verifyPasswordRequest);

            assertEquals("accessToken123", response.getAccessToken());
            assertEquals("refreshToken456", response.getRefreshToken());
            assertEquals(UserFlowStatusEnum.COMPLETED, response.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when UserService throws")
        void shouldPropagateServiceException() {
            when(userService.verifyPassword(any(VerifyPasswordRequest.class)))
                    .thenThrow(new IllegalArgumentException("Invalid input"));

            assertThrows(IllegalArgumentException.class,
                    () -> controller.verifyPassword(verifyPasswordRequest));
        }

        @Test
        @DisplayName("Should handle different request objects")
        void shouldHandleDifferentRequests() {
            VerifyPasswordRequest request1 = VerifyPasswordRequest.builder()
                    .userId("user-1")
                    .password("pass1")
                    .build();

            VerifyPasswordRequest request2 = VerifyPasswordRequest.builder()
                    .userId("user-2")
                    .password("pass2")
                    .build();

            when(userService.verifyPassword(request1))
                    .thenReturn(UserAccessResponse.builder().status(UserFlowStatusEnum.COMPLETED).build());
            when(userService.verifyPassword(request2))
                    .thenReturn(UserAccessResponse.builder().status(UserFlowStatusEnum.WAITING_FOR_VERIFICATION).build());

            UserAccessResponse response1 = controller.verifyPassword(request1);
            UserAccessResponse response2 = controller.verifyPassword(request2);

            assertEquals(UserFlowStatusEnum.COMPLETED, response1.getStatus());
            assertEquals(UserFlowStatusEnum.WAITING_FOR_VERIFICATION, response2.getStatus());
        }
    }
}
