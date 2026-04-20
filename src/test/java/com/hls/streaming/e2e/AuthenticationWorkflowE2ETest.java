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
import org.junit.jupiter.api.Nested;
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
@DisplayName("User Authentication Workflow E2E Tests")
class AuthenticationWorkflowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Single User Authentication")
    class SingleUserAuthenticationTests {

        private UserDocument user;

        @BeforeEach
        void setUp() {
            user = UserDocument.builder()
                    .id("user-1")
                    .username("john")
                    .email("john@example.com")
                    .displayName("John Doe")
                    .password(passwordEncoder.encode("john123"))
                    .status(UserStatusEnum.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            userRepository.save(user);
        }

        @Test
        @DisplayName("Complete authentication flow with email")
        void completeAuthFlowWithEmail() throws Exception {
            IdentifyUserRequest identifyReq = IdentifyUserRequest.builder()
                    .identifier("john@example.com")
                    .build();

            MvcResult identifyResult = mockMvc.perform(post("/api/v1/users/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(identifyReq)))
                    .andExpect(status().isOk())
                    .andReturn();

            String identifyResponse = identifyResult.getResponse().getContentAsString();
            assertTrue(identifyResponse.contains("WAITING_FOR_VERIFICATION"));

            VerifyPasswordRequest verifyReq = VerifyPasswordRequest.builder()
                    .userId("user-1")
                    .password("john123")
                    .build();

            MvcResult verifyResult = mockMvc.perform(post("/api/v1/users/verify-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(verifyReq)))
                    .andExpect(status().isOk())
                    .andReturn();

            String verifyResponse = verifyResult.getResponse().getContentAsString();
            assertTrue(verifyResponse.contains("COMPLETED"));
            assertTrue(verifyResponse.contains("accessToken"));
            assertTrue(verifyResponse.contains("refreshToken"));
        }

        @Test
        @DisplayName("Complete authentication flow with username")
        void completeAuthFlowWithUsername() throws Exception {
            IdentifyUserRequest identifyReq = IdentifyUserRequest.builder()
                    .identifier("john")
                    .build();

            mockMvc.perform(post("/api/v1/users/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(identifyReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userInfo.username").value("john"));

            VerifyPasswordRequest verifyReq = VerifyPasswordRequest.builder()
                    .userId("user-1")
                    .password("john123")
                    .build();

            mockMvc.perform(post("/api/v1/users/verify-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(verifyReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }
    }

    @Nested
    @DisplayName("Multiple Users Authentication")
    class MultipleUsersAuthenticationTests {

        @BeforeEach
        void setUp() {
            UserDocument user1 = UserDocument.builder()
                    .id("user-1")
                    .username("alice")
                    .email("alice@example.com")
                    .displayName("Alice")
                    .password(passwordEncoder.encode("alice123"))
                    .status(UserStatusEnum.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            UserDocument user2 = UserDocument.builder()
                    .id("user-2")
                    .username("bob")
                    .email("bob@example.com")
                    .displayName("Bob")
                    .password(passwordEncoder.encode("bob456"))
                    .status(UserStatusEnum.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            userRepository.save(user1);
            userRepository.save(user2);
        }

        @Test
        @DisplayName("Two users can authenticate independently")
        void twoUsersCanAuthenticateIndependently() throws Exception {
            IdentifyUserRequest aliceIdentify = IdentifyUserRequest.builder()
                    .identifier("alice@example.com")
                    .build();

            IdentifyUserRequest bobIdentify = IdentifyUserRequest.builder()
                    .identifier("bob@example.com")
                    .build();

            mockMvc.perform(post("/api/v1/users/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(aliceIdentify)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userInfo.username").value("alice"));

            mockMvc.perform(post("/api/v1/users/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bobIdentify)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userInfo.username").value("bob"));
        }

        @Test
        @DisplayName("User cannot authenticate with another user's password")
        void userCannotUseAnotherUsersPassword() throws Exception {
            VerifyPasswordRequest bobWithAlicePassword = VerifyPasswordRequest.builder()
                    .userId("user-2")
                    .password("alice123")
                    .build();

            mockMvc.perform(post("/api/v1/users/verify-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bobWithAlicePassword)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingAndEdgeCases {

        private UserDocument activeUser;
        private UserDocument inactiveUser;
        private UserDocument blockedUser;

        @BeforeEach
        void setUp() {
            activeUser = UserDocument.builder()
                    .id("active-user")
                    .username("active")
                    .email("active@example.com")
                    .displayName("Active User")
                    .password(passwordEncoder.encode("password"))
                    .status(UserStatusEnum.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            inactiveUser = UserDocument.builder()
                    .id("inactive-user")
                    .username("inactive")
                    .email("inactive@example.com")
                    .displayName("Inactive User")
                    .password(passwordEncoder.encode("password"))
                    .status(UserStatusEnum.INACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            blockedUser = UserDocument.builder()
                    .id("blocked-user")
                    .username("blocked")
                    .email("blocked@example.com")
                    .displayName("Blocked User")
                    .password(passwordEncoder.encode("password"))
                    .status(UserStatusEnum.BLOCKED)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            userRepository.save(activeUser);
            userRepository.save(inactiveUser);
            userRepository.save(blockedUser);
        }

        @Test
        @DisplayName("Inactive user cannot identify")
        void inactiveUserCannotIdentify() throws Exception {
            IdentifyUserRequest request = IdentifyUserRequest.builder()
                    .identifier("inactive@example.com")
                    .build();

            mockMvc.perform(post("/api/v1/users/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Blocked user cannot identify")
        void blockedUserCannotIdentify() throws Exception {
            IdentifyUserRequest request = IdentifyUserRequest.builder()
                    .identifier("blocked@example.com")
                    .build();

            mockMvc.perform(post("/api/v1/users/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Non-existent user returns 404")
        void nonExistentUserReturns404() throws Exception {
            IdentifyUserRequest request = IdentifyUserRequest.builder()
                    .identifier("nonexistent@example.com")
                    .build();

            mockMvc.perform(post("/api/v1/users/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Empty identifier returns 400")
        void emptyIdentifierReturns400() throws Exception {
            IdentifyUserRequest request = IdentifyUserRequest.builder()
                    .identifier("")
                    .build();

            mockMvc.perform(post("/api/v1/users/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Null identifier returns 400")
        void nullIdentifierReturns400() throws Exception {
            IdentifyUserRequest request = IdentifyUserRequest.builder()
                    .identifier(null)
                    .build();

            mockMvc.perform(post("/api/v1/users/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Case sensitive email matching")
        void caseSensitiveEmailMatching() throws Exception {
            IdentifyUserRequest request = IdentifyUserRequest.builder()
                    .identifier("ACTIVE@EXAMPLE.COM")
                    .build();

            mockMvc.perform(post("/api/v1/users/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Request/Response Validation")
    class RequestResponseValidation {

        private UserDocument user;

        @BeforeEach
        void setUp() {
            user = UserDocument.builder()
                    .id("user-1")
                    .username("testuser")
                    .email("test@example.com")
                    .displayName("Test User")
                    .password(passwordEncoder.encode("password123"))
                    .status(UserStatusEnum.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            userRepository.save(user);
        }

        @Test
        @DisplayName("Identify response contains required fields")
        void identifyResponseContainsRequiredFields() throws Exception {
            IdentifyUserRequest request = IdentifyUserRequest.builder()
                    .identifier("test@example.com")
                    .build();

            mockMvc.perform(post("/api/v1/users/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.passwordVerificationToken").exists())
                    .andExpect(jsonPath("$.userInfo").exists())
                    .andExpect(jsonPath("$.userInfo.username").exists());
        }

        @Test
        @DisplayName("Verify response contains required tokens")
        void verifyResponseContainsRequiredTokens() throws Exception {
            VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                    .userId("user-1")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/v1/users/verify-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists());
        }

        @Test
        @DisplayName("Response has correct content type")
        void responseHasCorrectContentType() throws Exception {
            IdentifyUserRequest request = IdentifyUserRequest.builder()
                    .identifier("test@example.com")
                    .build();

            mockMvc.perform(post("/api/v1/users/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Blank password in verify request returns 400")
        void blankPasswordReturns400() throws Exception {
            VerifyPasswordRequest request = VerifyPasswordRequest.builder()
                    .userId("user-1")
                    .password("   ")
                    .build();

            mockMvc.perform(post("/api/v1/users/verify-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Missing required fields returns 400")
        void missingRequiredFieldsReturns400() throws Exception {
            String invalidRequest = "{\"identifier\":null}";

            mockMvc.perform(post("/api/v1/users/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }
}
