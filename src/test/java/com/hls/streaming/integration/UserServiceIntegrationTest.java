package com.hls.streaming.integration;

import com.hls.streaming.documents.user.UserDocument;
import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.dtos.user.IdentifyUserRequest;
import com.hls.streaming.dtos.user.VerifyPasswordRequest;
import com.hls.streaming.enums.UserFlowStatusEnum;
import com.hls.streaming.enums.UserStatusEnum;
import com.hls.streaming.repositories.user.UserRepository;
import com.hls.streaming.services.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserService Integration Tests")
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserDocument testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        String encodedPassword = passwordEncoder.encode("password123");
        testUser = UserDocument.builder()
                .id("user-123")
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .password(encodedPassword)
                .avatar("https://example.com/avatar.jpg")
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
    @DisplayName("Should identify user by email and generate password verification token")
    void shouldIdentifyUserByEmail() {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        UserAccessResponse response = userService.identifyUser(request);

        assertNotNull(response);
        assertEquals(UserFlowStatusEnum.WAITING_FOR_VERIFICATION, response.getStatus());
        assertNotNull(response.getPasswordVerificationToken());
        assertNotNull(response.getUserInfo());
        assertEquals("testuser", response.getUserInfo().getUsername());
        assertEquals("test@example.com", response.getUserInfo().getId());
    }

    @Test
    @DisplayName("Should identify user by username when email is not found")
    void shouldIdentifyUserByUsername() {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("testuser")
                .build();

        UserAccessResponse response = userService.identifyUser(request);

        assertNotNull(response);
        assertEquals(UserFlowStatusEnum.WAITING_FOR_VERIFICATION, response.getStatus());
        assertNotNull(response.getPasswordVerificationToken());
    }

    @Test
    @DisplayName("Should complete password verification flow")
    void shouldCompletePasswordVerificationFlow() {
        IdentifyUserRequest identifyRequest = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        UserAccessResponse identifyResponse = userService.identifyUser(identifyRequest);
        assertNotNull(identifyResponse.getPasswordVerificationToken());

        VerifyPasswordRequest verifyRequest = VerifyPasswordRequest.builder()
                .userId("user-123")
                .password("password123")
                .build();

        UserAccessResponse verifyResponse = userService.verifyPassword(verifyRequest);

        assertNotNull(verifyResponse);
        assertEquals(UserFlowStatusEnum.COMPLETED, verifyResponse.getStatus());
        assertNotNull(verifyResponse.getAccessToken());
        assertNotNull(verifyResponse.getRefreshToken());
    }

    @Test
    @DisplayName("Should fail verification with incorrect password")
    void shouldFailVerificationWithIncorrectPassword() {
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .userId("user-123")
                .password("wrongPassword")
                .build();

        assertThrows(Exception.class, () -> userService.verifyPassword(request));
    }

    @Test
    @DisplayName("Should not identify inactive user")
    void shouldNotIdentifyInactiveUser() {
        testUser.setStatus(UserStatusEnum.INACTIVE);
        userRepository.save(testUser);

        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        assertThrows(Exception.class, () -> userService.identifyUser(request));
    }

    @Test
    @DisplayName("Should handle multiple users in database")
    void shouldHandleMultipleUsers() {
        UserDocument secondUser = UserDocument.builder()
                .id("user-456")
                .username("seconduser")
                .email("second@example.com")
                .displayName("Second User")
                .password(passwordEncoder.encode("password456"))
                .status(UserStatusEnum.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        userRepository.save(secondUser);

        IdentifyUserRequest firstRequest = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        IdentifyUserRequest secondRequest = IdentifyUserRequest.builder()
                .identifier("second@example.com")
                .build();

        UserAccessResponse firstResponse = userService.identifyUser(firstRequest);
        UserAccessResponse secondResponse = userService.identifyUser(secondRequest);

        assertNotNull(firstResponse);
        assertNotNull(secondResponse);
        assertNotEquals(firstResponse.getUserInfo().getId(), secondResponse.getUserInfo().getId());
    }

    @Test
    @DisplayName("Should find user with unique email index")
    void shouldFindUserWithUniqueEmailIndex() {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        UserAccessResponse response = userService.identifyUser(request);

        assertNotNull(response);
        assertEquals(UserFlowStatusEnum.WAITING_FOR_VERIFICATION, response.getStatus());
    }

    @Test
    @DisplayName("Should find user with unique username index")
    void shouldFindUserWithUniqueUsernameIndex() {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("testuser")
                .build();

        UserAccessResponse response = userService.identifyUser(request);

        assertNotNull(response);
        assertEquals(UserFlowStatusEnum.WAITING_FOR_VERIFICATION, response.getStatus());
    }
}
