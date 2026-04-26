package com.hls.streaming.integration;

import com.hls.streaming.documents.user.User;
import com.hls.streaming.enums.UserStatusEnum;
import com.hls.streaming.repositories.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserRepository Integration Tests")
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        String encodedPassword = passwordEncoder.encode("password123");
        testUser = User.builder()
                .id("user-123")
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .password(encodedPassword)
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
    @DisplayName("Should find user by username")
    void shouldFindUserByUsername() {
        Optional<User> foundUser = userRepository.findByUsername("testuser");

        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
        assertEquals("test@example.com", foundUser.get().getEmail());
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
        assertEquals("test@example.com", foundUser.get().getEmail());
    }

    @Test
    @DisplayName("Should not find user with non-existent username")
    void shouldNotFindNonExistentUsername() {
        Optional<User> foundUser = userRepository.findByUsername("nonexistent");

        assertTrue(foundUser.isEmpty());
    }

    @Test
    @DisplayName("Should not find user with non-existent email")
    void shouldNotFindNonExistentEmail() {
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        assertTrue(foundUser.isEmpty());
    }

    @Test
    @DisplayName("Should find user by ID")
    void shouldFindUserById() {
        Optional<User> foundUser = userRepository.findById("user-123");

        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
    }

    @Test
    @DisplayName("Should check if username exists")
    void shouldCheckUsernameExists() {
        boolean exists = userRepository.existsByUsername("testuser");

        assertTrue(exists);
    }

    @Test
    @DisplayName("Should check if username does not exist")
    void shouldCheckUsernameDoesNotExist() {
        boolean exists = userRepository.existsByUsername("nonexistent");

        assertFalse(exists);
    }

    @Test
    @DisplayName("Should find user with custom email query")
    void shouldFindUserWithCustomEmailQuery() {
        Optional<User> foundUser = userRepository.findCustomUserByEmail("test@example.com");

        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
    }

    @Test
    @DisplayName("Should save new user")
    void shouldSaveNewUser() {
        User newUser = User.builder()
                .id("user-456")
                .username("newuser")
                .email("new@example.com")
                .displayName("New User")
                .password(passwordEncoder.encode("password456"))
                .status(UserStatusEnum.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User savedUser = userRepository.save(newUser);

        assertNotNull(savedUser);
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("new@example.com", savedUser.getEmail());

        Optional<User> foundUser = userRepository.findByUsername("newuser");
        assertTrue(foundUser.isPresent());
    }

    @Test
    @DisplayName("Should update existing user")
    void shouldUpdateExistingUser() {
        testUser.setDisplayName("Updated Name");
        testUser.setStatus(UserStatusEnum.INACTIVE);

        User updatedUser = userRepository.save(testUser);

        assertEquals("Updated Name", updatedUser.getDisplayName());
        assertEquals(UserStatusEnum.INACTIVE, updatedUser.getStatus());

        Optional<User> foundUser = userRepository.findByUsername("testuser");
        assertEquals("Updated Name", foundUser.get().getDisplayName());
        assertEquals(UserStatusEnum.INACTIVE, foundUser.get().getStatus());
    }

    @Test
    @DisplayName("Should delete user")
    void shouldDeleteUser() {
        userRepository.delete(testUser);

        Optional<User> foundUser = userRepository.findByUsername("testuser");

        assertTrue(foundUser.isEmpty());
    }

    @Test
    @DisplayName("Should get all users")
    void shouldGetAllUsers() {
        User secondUser = User.builder()
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

        List<User> users = userRepository.findAll();

        assertEquals(2, users.size());
    }

    @Test
    @DisplayName("Should handle null optional gracefully")
    void shouldHandleNullOptionalGracefully() {
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        assertFalse(foundUser.isPresent());
        assertTrue(foundUser.isEmpty());
    }

    @Test
    @DisplayName("Should maintain username uniqueness constraint")
    void shouldMaintainUsernameUniqueness() {
        User duplicateUser = User.builder()
                .id("user-duplicate")
                .username("testuser")
                .email("duplicate@example.com")
                .displayName("Duplicate User")
                .password(passwordEncoder.encode("password"))
                .status(UserStatusEnum.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        assertThrows(Exception.class, () -> userRepository.save(duplicateUser));
    }

    @Test
    @DisplayName("Should maintain email uniqueness constraint")
    void shouldMaintainEmailUniqueness() {
        User duplicateUser = User.builder()
                .id("user-duplicate")
                .username("duplicateuser")
                .email("test@example.com")
                .displayName("Duplicate User")
                .password(passwordEncoder.encode("password"))
                .status(UserStatusEnum.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        assertThrows(Exception.class, () -> userRepository.save(duplicateUser));
    }

    @Test
    @DisplayName("Should handle status enum properly")
    void shouldHandleStatusEnum() {
        userRepository.deleteAll();

        User activeUser = User.builder()
                .id("active-user")
                .username("activeuser")
                .email("active@example.com")
                .displayName("Active User")
                .password(passwordEncoder.encode("password"))
                .status(UserStatusEnum.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User blockedUser = User.builder()
                .id("blocked-user")
                .username("blockeduser")
                .email("blocked@example.com")
                .displayName("Blocked User")
                .password(passwordEncoder.encode("password"))
                .status(UserStatusEnum.BLOCKED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        userRepository.save(activeUser);
        userRepository.save(blockedUser);

        List<User> allUsers = userRepository.findAll();
        assertEquals(2, allUsers.size());
        assertTrue(allUsers.stream().anyMatch(u -> u.getStatus() == UserStatusEnum.ACTIVE));
        assertTrue(allUsers.stream().anyMatch(u -> u.getStatus() == UserStatusEnum.BLOCKED));
    }
}
