package com.hls.streaming.unit;

import com.hls.streaming.documents.user.UserDocument;
import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.dtos.user.IdentifyUserRequest;
import com.hls.streaming.dtos.user.VerifyPasswordRequest;
import com.hls.streaming.enums.UserFlowStatusEnum;
import com.hls.streaming.enums.UserStatusEnum;
import com.hls.streaming.exception.BadRequestException;
import com.hls.streaming.exception.NotFoundException;
import com.hls.streaming.repositories.user.UserRepository;
import com.hls.streaming.services.token.TokenService;
import com.hls.streaming.services.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserDocument testUser;
    private IdentifyUserRequest identifyUserRequest;
    private VerifyPasswordRequest verifyPasswordRequest;

    @BeforeEach
    void setUp() {
        testUser = UserDocument.builder()
                .id("user-123")
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .password("hashedPassword123")
                .avatar("https://example.com/avatar.jpg")
                .status(UserStatusEnum.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        identifyUserRequest = IdentifyUserRequest.builder()
                .identifier("test@example.com")
                .build();

        verifyPasswordRequest = VerifyPasswordRequest.builder()
                .userId("user-123")
                .password("password123")
                .build();
    }

    @Nested
    @DisplayName("identifyUser Tests")
    class IdentifyUserTests {

        @Test
        @DisplayName("Should successfully identify user by email")
        void shouldIdentifyUserByEmail() {
            when(userRepository.findByEmail("test@example.com"))
                    .thenReturn(Optional.of(testUser));
            when(tokenService.generateToken(eq(testUser), any()))
                    .thenReturn("tempToken123");

            UserAccessResponse response = userService.identifyUser(identifyUserRequest);

            assertNotNull(response);
            assertEquals(UserFlowStatusEnum.WAITING_FOR_VERIFICATION, response.getStatus());
            assertEquals("tempToken123", response.getPasswordVerificationToken());
            assertNotNull(response.getUserInfo());
            assertEquals("testuser", response.getUserInfo().getUsername());

            verify(userRepository, times(1)).findByEmail("test@example.com");
            verify(tokenService, times(1)).generateToken(testUser, any());
        }

        @Test
        @DisplayName("Should successfully identify user by username when email not found")
        void shouldIdentifyUserByUsername() {
            identifyUserRequest.setIdentifier("testuser");
            when(userRepository.findByEmail("testuser"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByUsername("testuser"))
                    .thenReturn(Optional.of(testUser));
            when(tokenService.generateToken(eq(testUser), any()))
                    .thenReturn("tempToken123");

            UserAccessResponse response = userService.identifyUser(identifyUserRequest);

            assertNotNull(response);
            assertEquals(UserFlowStatusEnum.WAITING_FOR_VERIFICATION, response.getStatus());
            assertEquals("testuser", response.getUserInfo().getUsername());

            verify(userRepository, times(1)).findByEmail("testuser");
            verify(userRepository, times(1)).findByUsername("testuser");
        }

        @Test
        @DisplayName("Should throw BadRequestException when identifier is blank")
        void shouldThrowBadRequestWhenIdentifierBlank() {
            identifyUserRequest.setIdentifier("");

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> userService.identifyUser(identifyUserRequest));

            assertEquals("Identifier cannot be empty", exception.getMessage());
            verify(userRepository, never()).findByEmail(anyString());
            verify(userRepository, never()).findByUsername(anyString());
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void shouldThrowNotFoundWhenUserNotFound() {
            when(userRepository.findByEmail("nonexistent@example.com"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByUsername("nonexistent@example.com"))
                    .thenReturn(Optional.empty());

            NotFoundException exception = assertThrows(NotFoundException.class,
                    () -> userService.identifyUser(identifyUserRequest));

            assertEquals("User not found", exception.getMessage());
            verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
            verify(userRepository, times(1)).findByUsername("nonexistent@example.com");
        }

        @Test
        @DisplayName("Should throw BadRequestException when user is inactive")
        void shouldThrowBadRequestWhenUserInactive() {
            testUser.setStatus(UserStatusEnum.INACTIVE);
            when(userRepository.findByEmail("test@example.com"))
                    .thenReturn(Optional.of(testUser));

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> userService.identifyUser(identifyUserRequest));

            assertEquals("User is currently inactive", exception.getMessage());
            verify(tokenService, never()).generateToken(any(), any());
        }

        @Test
        @DisplayName("Should throw BadRequestException when user is blocked")
        void shouldThrowBadRequestWhenUserBlocked() {
            testUser.setStatus(UserStatusEnum.BLOCKED);
            when(userRepository.findByEmail("test@example.com"))
                    .thenReturn(Optional.of(testUser));

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> userService.identifyUser(identifyUserRequest));

            assertEquals("User is currently inactive", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw BadRequestException when user is deleted")
        void shouldThrowBadRequestWhenUserDeleted() {
            testUser.setStatus(UserStatusEnum.DELETED);
            when(userRepository.findByEmail("test@example.com"))
                    .thenReturn(Optional.of(testUser));

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> userService.identifyUser(identifyUserRequest));

            assertEquals("User is currently inactive", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("verifyPassword Tests")
    class VerifyPasswordTests {

        @Test
        @DisplayName("Should successfully verify password and return tokens")
        void shouldVerifyPasswordSuccessfully() {
            UserAccessResponse expectedTokenResponse = UserAccessResponse.builder()
                    .accessToken("accessToken123")
                    .refreshToken("refreshToken456")
                    .status(UserFlowStatusEnum.COMPLETED)
                    .build();

            when(userRepository.findById("user-123"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword123"))
                    .thenReturn(true);
            when(tokenService.generateAccessTokenPair(testUser))
                    .thenReturn(expectedTokenResponse);

            UserAccessResponse response = userService.verifyPassword(verifyPasswordRequest);

            assertNotNull(response);
            assertEquals(UserFlowStatusEnum.COMPLETED, response.getStatus());
            assertEquals("accessToken123", response.getAccessToken());
            assertEquals("refreshToken456", response.getRefreshToken());

            verify(userRepository, times(1)).findById("user-123");
            verify(passwordEncoder, times(1)).matches("password123", "hashedPassword123");
            verify(tokenService, times(1)).generateAccessTokenPair(testUser);
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void shouldThrowNotFoundWhenUserNotExist() {
            when(userRepository.findById("nonexistent"))
                    .thenReturn(Optional.empty());

            NotFoundException exception = assertThrows(NotFoundException.class,
                    () -> userService.verifyPassword(verifyPasswordRequest));

            assertEquals("User not found", exception.getMessage());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(tokenService, never()).generateAccessTokenPair(any());
        }

        @Test
        @DisplayName("Should throw BadRequestException when password is invalid")
        void shouldThrowBadRequestWhenPasswordInvalid() {
            when(userRepository.findById("user-123"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", "hashedPassword123"))
                    .thenReturn(false);

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> userService.verifyPassword(verifyPasswordRequest));

            assertEquals("Invalid password", exception.getMessage());
            verify(tokenService, never()).generateAccessTokenPair(any());
        }

        @Test
        @DisplayName("Should handle null password gracefully")
        void shouldHandleNullPassword() {
            verifyPasswordRequest.setPassword(null);
            when(userRepository.findById("user-123"))
                    .thenReturn(Optional.of(testUser));

            assertThrows(Exception.class,
                    () -> userService.verifyPassword(verifyPasswordRequest));
        }
    }
}
