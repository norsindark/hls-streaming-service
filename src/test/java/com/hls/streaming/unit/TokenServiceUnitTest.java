package com.hls.streaming.unit;

import com.hls.streaming.documents.user.User;
import com.hls.streaming.dtos.token.GenerateTokenRequest;
import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.enums.UserFlowStatusEnum;
import com.hls.streaming.enums.UserStatusEnum;
import com.hls.streaming.security.component.TokenSupporter;
import com.hls.streaming.security.models.TokenType;
import com.hls.streaming.security.models.UserRole;
import com.hls.streaming.services.token.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService Unit Tests")
class TokenServiceUnitTest {

    @Mock
    private TokenSupporter tokenGenerator;

    @InjectMocks
    private TokenService tokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-123")
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .password("hashedPassword123")
                .status(UserStatusEnum.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("generateAccessTokenPair Tests")
    class GenerateAccessTokenPairTests {

        @Test
        @DisplayName("Should generate both access and refresh tokens")
        void shouldGenerateAccessTokenPair() {
            when(tokenGenerator.generateToken(any(GenerateTokenRequest.class), eq(Set.of(UserRole.USER))))
                    .thenReturn("generatedToken")
                    .thenReturn("generatedRefreshToken");

            UserAccessResponse response = tokenService.generateAccessTokenPair(testUser);

            assertNotNull(response);
            assertEquals(UserFlowStatusEnum.COMPLETED, response.getStatus());
            assertEquals("generatedToken", response.getAccessToken());
            assertEquals("generatedRefreshToken", response.getRefreshToken());

            verify(tokenGenerator, times(2)).generateToken(any(GenerateTokenRequest.class), any());
        }

        @Test
        @DisplayName("Should include USER role when generating tokens")
        void shouldIncludeUserRole() {
            when(tokenGenerator.generateToken(any(GenerateTokenRequest.class), any()))
                    .thenReturn("token1")
                    .thenReturn("token2");

            tokenService.generateAccessTokenPair(testUser);

            verify(tokenGenerator, times(2)).generateToken(any(), argThat(roles ->
                    roles.contains(UserRole.USER) && roles.size() == 1
            ));
        }

        @Test
        @DisplayName("Should use correct user ID when generating tokens")
        void shouldUseCorrectUserId() {
            when(tokenGenerator.generateToken(any(GenerateTokenRequest.class), any()))
                    .thenReturn("token1")
                    .thenReturn("token2");

            tokenService.generateAccessTokenPair(testUser);

            verify(tokenGenerator, times(2)).generateToken(
                    argThat(req -> req.getUserId().equals("user-123")),
                    any()
            );
        }

        @Test
        @DisplayName("Should set status to COMPLETED")
        void shouldSetCompletedStatus() {
            when(tokenGenerator.generateToken(any(GenerateTokenRequest.class), any()))
                    .thenReturn("token1")
                    .thenReturn("token2");

            UserAccessResponse response = tokenService.generateAccessTokenPair(testUser);

            assertEquals(UserFlowStatusEnum.COMPLETED, response.getStatus());
        }
    }

    @Nested
    @DisplayName("generateToken Tests")
    class GenerateTokenTests {

        @Test
        @DisplayName("Should generate single token for password verification")
        void shouldGeneratePasswordVerificationToken() {
            when(tokenGenerator.generateToken(any(GenerateTokenRequest.class), eq(Set.of(UserRole.USER))))
                    .thenReturn("passwordVerificationToken");

            String token = tokenService.generateToken(testUser, TokenType.PASSWORD_VERIFICATION_TOKEN);

            assertNotNull(token);
            assertEquals("passwordVerificationToken", token);
            verify(tokenGenerator, times(1)).generateToken(any(GenerateTokenRequest.class), any());
        }

        @Test
        @DisplayName("Should generate token with correct token type")
        void shouldGenerateTokenWithCorrectType() {
            when(tokenGenerator.generateToken(any(GenerateTokenRequest.class), any()))
                    .thenReturn("token");

            tokenService.generateToken(testUser, TokenType.PASSWORD_VERIFICATION_TOKEN);

            verify(tokenGenerator).generateToken(
                    argThat(req -> req.getTokenType() == TokenType.PASSWORD_VERIFICATION_TOKEN),
                    any()
            );
        }

        @Test
        @DisplayName("Should generate token with correct user ID")
        void shouldGenerateTokenWithCorrectUserId() {
            when(tokenGenerator.generateToken(any(GenerateTokenRequest.class), any()))
                    .thenReturn("token");

            tokenService.generateToken(testUser, TokenType.ACCESS_TOKEN);

            verify(tokenGenerator).generateToken(
                    argThat(req -> req.getUserId().equals("user-123")),
                    any()
            );
        }

        @Test
        @DisplayName("Should generate refresh token")
        void shouldGenerateRefreshToken() {
            when(tokenGenerator.generateToken(any(GenerateTokenRequest.class), any()))
                    .thenReturn("refreshToken");

            String token = tokenService.generateToken(testUser, TokenType.REFRESH_TOKEN);

            assertEquals("refreshToken", token);
            verify(tokenGenerator).generateToken(
                    argThat(req -> req.getTokenType() == TokenType.REFRESH_TOKEN),
                    any()
            );
        }

        @Test
        @DisplayName("Should always include USER role")
        void shouldAlwaysIncludeUserRole() {
            when(tokenGenerator.generateToken(any(GenerateTokenRequest.class), any()))
                    .thenReturn("token");

            tokenService.generateToken(testUser, TokenType.ACCESS_TOKEN);

            verify(tokenGenerator).generateToken(any(),
                    argThat(roles -> roles.contains(UserRole.USER)));
        }
    }
}
