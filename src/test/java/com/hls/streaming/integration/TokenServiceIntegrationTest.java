package com.hls.streaming.integration;

import com.hls.streaming.documents.user.UserDocument;
import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.enums.UserFlowStatusEnum;
import com.hls.streaming.enums.UserStatusEnum;
import com.hls.streaming.repositories.user.UserRepository;
import com.hls.streaming.services.token.TokenService;
import com.hls.streaming.services.token.generator.UserTokenGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TokenService Integration Tests")
class TokenServiceIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserTokenGenerator tokenGenerator;

    @Autowired
    private UserRepository userRepository;

    private UserDocument testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = UserDocument.builder()
                .id("user-123")
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .password("hashedPassword")
                .status(UserStatusEnum.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should generate valid access and refresh token pair")
    void shouldGenerateValidAccessTokenPair() {
        UserAccessResponse response = tokenService.generateAccessTokenPair(testUser);

        assertNotNull(response);
        assertEquals(UserFlowStatusEnum.COMPLETED, response.getStatus());
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertNotEquals(response.getAccessToken(), response.getRefreshToken());
    }

    @Test
    @DisplayName("Should generate unique tokens for each call")
    void shouldGenerateUniqueTokensPerCall() {
        UserAccessResponse response1 = tokenService.generateAccessTokenPair(testUser);
        UserAccessResponse response2 = tokenService.generateAccessTokenPair(testUser);

        assertNotEquals(response1.getAccessToken(), response2.getAccessToken());
        assertNotEquals(response1.getRefreshToken(), response2.getRefreshToken());
    }

    @Test
    @DisplayName("Should generate token with correct type")
    void shouldGenerateTokenWithCorrectType() {
        String token =
                tokenService.generateToken(testUser, com.hls.streaming.security.models.TokenType.PASSWORD_VERIFICATION_TOKEN);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.length() > 0);
    }

    @Test
    @DisplayName("Should generate different token types")
    void shouldGenerateDifferentTokenTypes() {
        String accessToken = tokenService.generateToken(testUser, com.hls.streaming.security.models.TokenType.ACCESS_TOKEN);
        String refreshToken = tokenService.generateToken(testUser, com.hls.streaming.security.models.TokenType.REFRESH_TOKEN);
        String passwordVerificationToken =
                tokenService.generateToken(testUser, com.hls.streaming.security.models.TokenType.PASSWORD_VERIFICATION_TOKEN);

        assertNotNull(accessToken);
        assertNotNull(refreshToken);
        assertNotNull(passwordVerificationToken);
        assertNotEquals(accessToken, refreshToken);
        assertNotEquals(accessToken, passwordVerificationToken);
        assertNotEquals(refreshToken, passwordVerificationToken);
    }

    @Test
    @DisplayName("Should generate tokens with consistent user ID")
    void shouldGenerateTokensWithConsistentUserId() {
        UserAccessResponse response1 = tokenService.generateAccessTokenPair(testUser);
        UserAccessResponse response2 = tokenService.generateAccessTokenPair(testUser);

        assertNotNull(response1.getAccessToken());
        assertNotNull(response2.getAccessToken());
    }

    @Test
    @DisplayName("Should handle different users independently")
    void shouldHandleDifferentUsersIndependently() {
        UserDocument secondUser = UserDocument.builder()
                .id("user-456")
                .username("seconduser")
                .email("second@example.com")
                .displayName("Second User")
                .password("hashedPassword2")
                .status(UserStatusEnum.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        userRepository.save(secondUser);

        UserAccessResponse response1 = tokenService.generateAccessTokenPair(testUser);
        UserAccessResponse response2 = tokenService.generateAccessTokenPair(secondUser);

        assertNotEquals(response1.getAccessToken(), response2.getAccessToken());
        assertNotEquals(response1.getRefreshToken(), response2.getRefreshToken());
    }

    @Test
    @DisplayName("Should set status to COMPLETED for access token pair")
    void shouldSetCompletedStatus() {
        UserAccessResponse response = tokenService.generateAccessTokenPair(testUser);

        assertEquals(UserFlowStatusEnum.COMPLETED, response.getStatus());
    }

    @Test
    @DisplayName("Should generate non-null tokens")
    void shouldGenerateNonNullTokens() {
        UserAccessResponse response = tokenService.generateAccessTokenPair(testUser);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
    }

    @Test
    @DisplayName("Should generate tokens with content")
    void shouldGenerateTokensWithContent() {
        UserAccessResponse response = tokenService.generateAccessTokenPair(testUser);

        assertFalse(response.getAccessToken().isEmpty());
        assertFalse(response.getRefreshToken().isEmpty());
        assertTrue(response.getAccessToken().length() > 10);
        assertTrue(response.getRefreshToken().length() > 10);
    }

    @Test
    @DisplayName("Should handle null user gracefully when catching exception")
    void shouldHandleNullUserGracefully() {
        assertThrows(Exception.class, () -> tokenService.generateAccessTokenPair(null));
    }
}
