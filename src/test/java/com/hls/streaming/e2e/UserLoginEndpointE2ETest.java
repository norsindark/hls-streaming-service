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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("User Login Endpoint Integration Tests")
class UserLoginEndpointE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserDocument primaryUser;
    private UserDocument secondaryUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        primaryUser = UserDocument.builder()
                .id("primary-user")
                .username("primary")
                .email("primary@example.com")
                .displayName("Primary User")
                .password(passwordEncoder.encode("primaryPass123"))
                .avatar("https://example.com/primary.jpg")
                .status(UserStatusEnum.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        secondaryUser = UserDocument.builder()
                .id("secondary-user")
                .username("secondary")
                .email("secondary@example.com")
                .displayName("Secondary User")
                .password(passwordEncoder.encode("secondaryPass456"))
                .status(UserStatusEnum.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        userRepository.save(primaryUser);
        userRepository.save(secondaryUser);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/users/identify with valid email returns 200")
    void identifyWithValidEmailReturns200() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("primary@example.com")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/users/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("WAITING_FOR_VERIFICATION"));
        assertTrue(responseBody.contains("passwordVerificationToken"));
    }

    @Test
    @DisplayName("POST /api/v1/users/identify with valid username returns 200")
    void identifyWithValidUsernameReturns200() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("primary")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userInfo.username").value("primary"));
    }

    @Test
    @DisplayName("POST /api/v1/users/identify with invalid email returns 404")
    void identifyWithInvalidEmailReturns404() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("invalid@example.com")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/users/identify with empty identifier returns 400")
    void identifyWithEmptyIdentifierReturns400() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/users/verify-password with correct password returns 200")
    void verifyPasswordWithCorrectPasswordReturns200() throws Exception {
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .userId("primary-user")
                .password("primaryPass123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/users/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("COMPLETED"));
        assertTrue(responseBody.contains("accessToken"));
        assertTrue(responseBody.contains("refreshToken"));
    }

    @Test
    @DisplayName("POST /api/v1/users/verify-password with incorrect password returns 400")
    void verifyPasswordWithIncorrectPasswordReturns400() throws Exception {
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .userId("primary-user")
                .password("wrongPassword")
                .build();

        mockMvc.perform(post("/api/v1/users/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/users/verify-password with non-existent user returns 404")
    void verifyPasswordWithNonExistentUserReturns404() throws Exception {
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .userId("non-existent-user")
                .password("anyPassword")
                .build();

        mockMvc.perform(post("/api/v1/users/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/users/verify-password with empty password returns 400")
    void verifyPasswordWithEmptyPasswordReturns400() throws Exception {
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .userId("primary-user")
                .password("")
                .build();

        mockMvc.perform(post("/api/v1/users/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/users/identify returns correct user info")
    void identifyReturnsCorrectUserInfo() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("primary@example.com")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userInfo.username").value("primary"))
                .andExpect(jsonPath("$.userInfo.displayName").value("Primary User"));
    }

    @Test
    @DisplayName("POST /api/v1/users/verify-password returns both tokens")
    void verifyPasswordReturnsBothTokens() throws Exception {
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .userId("primary-user")
                .password("primaryPass123")
                .build();

        mockMvc.perform(post("/api/v1/users/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Sequential identify and verify password calls succeed")
    void sequentialIdentifyAndVerifySucceed() throws Exception {
        IdentifyUserRequest identifyRequest = IdentifyUserRequest.builder()
                .identifier("secondary@example.com")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(identifyRequest)))
                .andExpect(status().isOk());

        VerifyPasswordRequest verifyRequest = VerifyPasswordRequest.builder()
                .userId("secondary-user")
                .password("secondaryPass456")
                .build();

        mockMvc.perform(post("/api/v1/users/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Identify response does not expose password")
    void identifyResponseDoesNotExposePassword() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("primary@example.com")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/users/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.contains("primaryPass123"));
    }

    @Test
    @DisplayName("Verify password response does not expose password")
    void verifyPasswordResponseDoesNotExposePassword() throws Exception {
        VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                .userId("primary-user")
                .password("primaryPass123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/users/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.contains("primaryPass123"));
    }

    @Test
    @DisplayName("Identify with whitespace identifier fails")
    void identifyWithWhitespaceIdentifierFails() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("   ")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Multiple consecutive identify requests are allowed")
    void multipleConsecutiveIdentifyRequestsAllowed() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("primary@example.com")
                .build();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/users/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Different users can authenticate in sequence")
    void differentUsersCanAuthenticateInSequence() throws Exception {
        IdentifyUserRequest primaryRequest = IdentifyUserRequest.builder()
                .identifier("primary@example.com")
                .build();

        IdentifyUserRequest secondaryRequest = IdentifyUserRequest.builder()
                .identifier("secondary@example.com")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(primaryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userInfo.username").value("primary"));

        mockMvc.perform(post("/api/v1/users/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondaryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userInfo.username").value("secondary"));
    }

    @Test
    @DisplayName("Invalid JSON in request returns 400")
    void invalidJsonInRequestReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/users/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST requests are accepted with correct endpoint paths")
    void postRequestsAcceptedWithCorrectPaths() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("primary@example.com")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Response content type is JSON")
    void responseContentTypeIsJson() throws Exception {
        IdentifyUserRequest request = IdentifyUserRequest.builder()
                .identifier("primary@example.com")
                .build();

        mockMvc.perform(post("/api/v1/users/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
