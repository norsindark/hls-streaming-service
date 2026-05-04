package com.hls.streaming.integration.user;

import com.hls.streaming.HlsApplication;
import com.hls.streaming.user.domain.document.User;
import com.hls.streaming.user.domain.enums.UserStatusEnum;
import com.hls.streaming.user.domain.repository.UserRepository;
import com.hls.streaming.user.dto.RegisterUserRequest;
import com.hls.streaming.security.authentication.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HlsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .displayName("Test User")
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.USER))
                .build();
    }

    @Test
    void register_WithValidRequest_ShouldCreateUserAndReturnTokens() throws Exception {
        // Given
        RegisterUserRequest request = RegisterUserRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("password123")
                .displayName("New User")
                .build();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("accessToken");
        assertThat(responseContent).contains("refreshToken");

        User savedUser = userRepository.findByUsername("newuser").orElseThrow();
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedUser.getStatus()).isEqualTo(UserStatusEnum.ACTIVE);
    }

    @Test
    void register_WithDuplicateEmail_ShouldReturnBadRequest() throws Exception {
        // Given
        userRepository.save(testUser);

        RegisterUserRequest request = RegisterUserRequest.builder()
                .username("anotheruser")
                .email("test@example.com")
                .password("password123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WithDuplicateUsername_ShouldReturnBadRequest() throws Exception {
        // Given
        userRepository.save(testUser);

        RegisterUserRequest request = RegisterUserRequest.builder()
                .username("testuser")
                .email("another@example.com")
                .password("password123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        // Given
        RegisterUserRequest request = RegisterUserRequest.builder()
                .username("newuser")
                .email("invalid-email")
                .password("password123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void identify_WithValidEmail_ShouldReturnVerificationToken() throws Exception {
        // Given
        userRepository.save(testUser);

        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("identifier", "test@example.com")
        );

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("passwordVerificationToken");
    }

    @Test
    void identify_WithValidUsername_ShouldReturnVerificationToken() throws Exception {
        // Given
        userRepository.save(testUser);

        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("identifier", "testuser")
        );

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("passwordVerificationToken");
    }

    @Test
    void identify_WithNonexistentUser_ShouldReturnNotFound() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("identifier", "nonexistent@example.com")
        );

        // When & Then
        mockMvc.perform(post("/api/v1/auth/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void register_WithoutDisplayName_ShouldUseUsername() throws Exception {
        // Given
        RegisterUserRequest request = RegisterUserRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("password123")
                .build();

        // When
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then
        User savedUser = userRepository.findByUsername("newuser").orElseThrow();
        assertThat(savedUser.getDisplayName()).isEqualTo("newuser");
    }
}
