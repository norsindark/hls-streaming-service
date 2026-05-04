package com.hls.streaming.integration.user;

import com.hls.streaming.HlsApplication;
import com.hls.streaming.user.domain.document.User;
import com.hls.streaming.user.domain.enums.UserStatusEnum;
import com.hls.streaming.user.domain.repository.UserRepository;
import com.hls.streaming.security.authentication.model.UserRole;
import com.hls.streaming.security.context.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HlsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .displayName("Test User")
                .avatar("https://example.com/avatar.jpg")
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.USER))
                .build();
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getProfile_WithAuthenticatedUser_ShouldReturnUserProfile() throws Exception {
        // Given
        User savedUser = userRepository.save(testUser);
        when(currentUserProvider.getUserId()).thenReturn(savedUser.getId());

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("testuser");
        assertThat(responseContent).contains("test@example.com");
        assertThat(responseContent).contains("Test User");
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getProfile_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getProfile_WithAdminUser_ShouldReturnAdminFlag() throws Exception {
        // Given
        User adminUser = User.builder()
                .username("testuser")
                .email("admin@example.com")
                .password(passwordEncoder.encode("password123"))
                .displayName("Admin User")
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.ADMIN, UserRole.USER))
                .build();

        User savedUser = userRepository.save(adminUser);
        when(currentUserProvider.getUserId()).thenReturn(savedUser.getId());

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("\"isAdmin\":true");
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getProfile_WithInactiveUser_ShouldReturnInactiveFlag() throws Exception {
        // Given
        User inactiveUser = User.builder()
                .username("testuser")
                .email("inactive@example.com")
                .password(passwordEncoder.encode("password123"))
                .status(UserStatusEnum.INACTIVE)
                .roles(Set.of(UserRole.USER))
                .build();

        User savedUser = userRepository.save(inactiveUser);
        when(currentUserProvider.getUserId()).thenReturn(savedUser.getId());

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("\"isActive\":false");
    }
}
