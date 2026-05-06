package com.hls.streaming.e2e.user;

import com.hls.streaming.HlsApplication;
import com.hls.streaming.config.NoSecurityConfig;
import com.hls.streaming.user.domain.document.User;
import com.hls.streaming.user.domain.enums.UserStatusEnum;
import com.hls.streaming.user.domain.repository.UserRepository;
import com.hls.streaming.security.authentication.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HlsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(NoSecurityConfig.class)
@Disabled("Temporarily disable E2E tests")
class UserAuthFlowE2ETest {

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

    @Test
    void completeUserAuthenticationFlow_ShouldSucceedEndToEnd() throws Exception {
        // Step 1: Register new user
        String registerRequest = objectMapper.writeValueAsString(
                java.util.Map.of(
                        "username", "e2euser",
                        "email", "e2euser@example.com",
                        "password", "SecurePass123!",
                        "displayName", "E2E Test User"
                )
        );

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isOk())
                .andReturn();

        String registerResponse = registerResult.getResponse().getContentAsString();
        JsonNode registerNode = objectMapper.readTree(registerResponse);

        String accessToken = registerNode.get("accessToken").asText();
        String refreshToken = registerNode.get("refreshToken").asText();

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // Step 2: Verify user was created in database
        User registeredUser = userRepository.findByUsername("e2euser").orElseThrow();
        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getEmail()).isEqualTo("e2euser@example.com");
        assertThat(registeredUser.getStatus()).isEqualTo(UserStatusEnum.ACTIVE);

        // Step 3: Access protected resource with token (if profile endpoint requires auth)
        MvcResult profileResult = mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String profileResponse = profileResult.getResponse().getContentAsString();
        assertThat(profileResponse).contains("e2euser");
    }

    @Test
    void userIdentificationAndPasswordVerificationFlow_ShouldSucceedEndToEnd() throws Exception {
        // Setup: Create existing user
        User existingUser = User.builder()
                .username("existinguser")
                .email("existing@example.com")
                .password(passwordEncoder.encode("password123"))
                .displayName("Existing User")
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.USER))
                .build();

        userRepository.save(existingUser);

        // Step 1: Identify user by email
        String identifyRequest = objectMapper.writeValueAsString(
                java.util.Map.of("identifier", "existing@example.com")
        );

        MvcResult identifyResult = mockMvc.perform(post("/api/v1/auth/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(identifyRequest))
                .andExpect(status().isOk())
                .andReturn();

        String identifyResponse = identifyResult.getResponse().getContentAsString();
        JsonNode identifyNode = objectMapper.readTree(identifyResponse);

        String passwordVerificationToken = identifyNode.get("passwordVerificationToken").asText();
        assertThat(passwordVerificationToken).isNotBlank();

        // Step 2: Verify password with the verification token
        String verifyRequest = objectMapper.writeValueAsString(
                java.util.Map.of("password", "password123")
        );

        MvcResult verifyResult = mockMvc.perform(post("/api/v1/auth/verify-password")
                .header("Authorization", "Bearer " + passwordVerificationToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyRequest))
                .andExpect(status().isOk())
                .andReturn();

        String verifyResponse = verifyResult.getResponse().getContentAsString();
        JsonNode verifyNode = objectMapper.readTree(verifyResponse);

        String accessToken = verifyNode.get("accessToken").asText();
        assertThat(accessToken).isNotBlank();
    }

    @Test
    void userIdentificationByUsername_ShouldSucceedEndToEnd() throws Exception {
        // Setup: Create existing user
        User existingUser = User.builder()
                .username("usernametest")
                .email("usernametest@example.com")
                .password(passwordEncoder.encode("password123"))
                .displayName("Username Test")
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.USER))
                .build();

        userRepository.save(existingUser);

        // Step 1: Identify user by username
        String identifyRequest = objectMapper.writeValueAsString(
                java.util.Map.of("identifier", "usernametest")
        );

        MvcResult identifyResult = mockMvc.perform(post("/api/v1/auth/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(identifyRequest))
                .andExpect(status().isOk())
                .andReturn();

        String identifyResponse = identifyResult.getResponse().getContentAsString();
        JsonNode identifyNode = objectMapper.readTree(identifyResponse);

        String passwordVerificationToken = identifyNode.get("passwordVerificationToken").asText();
        assertThat(passwordVerificationToken).isNotBlank();

        // Verify user info is returned
        JsonNode userInfo = identifyNode.get("userInfo");
        assertThat(userInfo.get("username").asText()).isEqualTo("usernametest");
    }

    @Test
    void multipleUserRegistration_ShouldSucceedIndependently() throws Exception {
        // Register first user
        String user1Request = objectMapper.writeValueAsString(
                java.util.Map.of(
                        "username", "user1",
                        "email", "user1@example.com",
                        "password", "password123"
                )
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(user1Request))
                .andExpect(status().isOk());

        // Register second user
        String user2Request = objectMapper.writeValueAsString(
                java.util.Map.of(
                        "username", "user2",
                        "email", "user2@example.com",
                        "password", "password123"
                )
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(user2Request))
                .andExpect(status().isOk());

        // Verify both users exist
        assertThat(userRepository.findByUsername("user1")).isPresent();
        assertThat(userRepository.findByUsername("user2")).isPresent();

        // Verify they are different users
        User user1 = userRepository.findByUsername("user1").orElseThrow();
        User user2 = userRepository.findByUsername("user2").orElseThrow();

        assertThat(user1.getId()).isNotEqualTo(user2.getId());
        assertThat(user1.getEmail()).isNotEqualTo(user2.getEmail());
    }

    @Test
    void duplicateEmailRegistration_ShouldFail() throws Exception {
        // Register first user
        String user1Request = objectMapper.writeValueAsString(
                java.util.Map.of(
                        "username", "user1",
                        "email", "duplicate@example.com",
                        "password", "password123"
                )
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(user1Request))
                .andExpect(status().isOk());

        // Try to register with same email
        String user2Request = objectMapper.writeValueAsString(
                java.util.Map.of(
                        "username", "user2",
                        "email", "duplicate@example.com",
                        "password", "password123"
                )
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(user2Request))
                .andExpect(status().isBadRequest());

        // Verify only first user exists
        assertThat(userRepository.findByEmail("duplicate@example.com")).isPresent();
//        assertThat(userRepository.countVideosByUserId("duplicate@example.com")).isZero();
    }
}
