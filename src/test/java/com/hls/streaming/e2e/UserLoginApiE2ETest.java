package com.hls.streaming.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hls.streaming.documents.user.UserDocument;
import com.hls.streaming.dtos.user.IdentifyUserRequest;
import com.hls.streaming.dtos.user.VerifyPasswordRequest;
import com.hls.streaming.enums.UserStatusEnum;
import com.hls.streaming.repositories.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("User Login API End-to-End Tests")
class UserLoginApiE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    @DisplayName("Should identify user by email and return temp token")
    void shouldIdentifyUserByEmail() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/users/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_VERIFICATION"))
                .andExpect(jsonPath("$.passwordVerificationToken").isNotEmpty())
                .andExpect(jsonPath("$.userInfo").isNotEmpty())
                .andExpect(jsonPath("$.userInfo.username").value("testuser"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("WAITING_FOR_VERIFICATION"));
    }

    @Test
    @DisplayName("Should identify user by username")
    void shouldIdentifyUserByUsername() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("testuser")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_VERIFICATION"))
                .andExpect(jsonPath("$.passwordVerificationToken").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 when identifier is empty")
    void shouldReturnBadRequestWhenIdentifierEmpty() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void shouldReturnNotFoundWhenUserNotFound() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("nonexistent@example.com")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when user is inactive")
    void shouldReturnBadRequestWhenUserInactive() throws Exception {
        testUser.setStatus(UserStatusEnum.INACTIVE);
        userRepository.save(testUser);

        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when user is blocked")
    void shouldReturnBadRequestWhenUserBlocked() throws Exception {
        testUser.setStatus(UserStatusEnum.BLOCKED);
        userRepository.save(testUser);

        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should verify password and return access tokens")
    void shouldVerifyPasswordAndReturnTokens() throws Exception {
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .userId("user-123")
                .password("password123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/users/verify-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("COMPLETED"));
        assertTrue(responseBody.contains("accessToken"));
        assertTrue(responseBody.contains("refreshToken"));
    }

    @Test
    @DisplayName("Should return 400 when password is incorrect")
    void shouldReturnBadRequestWhenPasswordIncorrect() throws Exception {
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .userId("user-123")
                .password("wrongPassword")
                .build();

        mockMvc.perform(post("/api/v1/users/verify-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 when user not found during verification")
    void shouldReturnNotFoundWhenUserNotFoundDuringVerification() throws Exception {
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .userId("nonexistent-user")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/users/verify-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when password is blank in verify request")
    void shouldReturnBadRequestWhenPasswordBlank() throws Exception {
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .userId("user-123")
                .password("")
                .build();

        mockMvc.perform(post("/api/v1/users/verify-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should complete full authentication flow")
    void shouldCompleteFullAuthenticationFlow() throws Exception {
        IdentifyUserRequest identifyRequest = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(identifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_VERIFICATION"));

        VerifyPasswordRequest verifyRequest = VerifyPasswordRequest.builder()
                .userId("user-123")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/users/verify-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("Should handle multiple identify requests for same user")
    void shouldHandleMultipleIdentifyRequests() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/users/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("WAITING_FOR_VERIFICATION"));
        }
    }

    @Test
    @DisplayName("Should return 400 when user ID is blank in verify")
    void shouldReturnBadRequestWhenUserIdBlank() throws Exception {
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .userId("")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/users/verify-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when request body is invalid JSON")
    void shouldReturnBadRequestWhenInvalidJson() throws Exception {
        mockMvc.perform(post("/api/v1/users/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return user info in identify response")
    void shouldReturnUserInfoInIdentifyResponse() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userInfo.username").value("testuser"))
                .andExpect(jsonPath("$.userInfo.displayName").value("Test User"));
    }

    @Test
    @DisplayName("Should not expose password in response")
    void shouldNotExposePasswordInResponse() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/users/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.contains("password123"));
        assertFalse(responseBody.contains("hashedPassword"));
    }
}
