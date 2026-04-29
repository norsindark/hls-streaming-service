package com.hls.streaming.services.user.internal;

import com.hls.streaming.config.error.ErrorCodeConfig;
import com.hls.streaming.constant.ErrorConfigConstants;
import com.hls.streaming.documents.user.User;
import com.hls.streaming.enums.UserStatusEnum;
import com.hls.streaming.exception.BadRequestException;
import com.hls.streaming.exception.NotFoundException;
import com.hls.streaming.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.hls.streaming.constant.ErrorConfigConstants.USER_REQUIRED_FIELDS_EMPTY;

@Component
@RequiredArgsConstructor
public class UserValidator {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ErrorCodeConfig errorCodeConfig;

    // ================= REGISTER =================

    public void validateRegisterInput(String username, String email, String password) {
        if (StringUtils.isBlank(username)
                || StringUtils.isBlank(email)
                || StringUtils.isBlank(password)) {
            throw new BadRequestException(errorCodeConfig.getMessage(USER_REQUIRED_FIELDS_EMPTY));
        }
    }

    public void validateUsernameNotExists(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException(errorCodeConfig.getMessage(ErrorConfigConstants.USERNAME_ALREADY_EXISTS));
        }
    }

    public void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException(errorCodeConfig.getMessage(ErrorConfigConstants.EMAIL_ALREADY_EXISTS));
        }
    }

    // ================= IDENTIFY =================

    public void validateIdentifier(String identifier) {
        if (StringUtils.isBlank(identifier)) {
            throw new BadRequestException(errorCodeConfig.getMessage(ErrorConfigConstants.IDENTIFIER_EMPTY));
        }
    }

    // ================= USER =================

    public void validateUserActive(User user) {
        if (!UserStatusEnum.ACTIVE.equals(user.getStatus())) {
            throw new BadRequestException(errorCodeConfig.getMessage(ErrorConfigConstants.USER_INACTIVE));
        }
    }

    public User requireUser(User user) {
        if (Objects.nonNull(user)) {
            throw new NotFoundException(errorCodeConfig.getMessage(ErrorConfigConstants.USER_NOT_FOUND));
        }
        return user;
    }

    // ================= PASSWORD =================

    public void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BadRequestException(errorCodeConfig.getMessage(ErrorConfigConstants.INVALID_PASSWORD));
        }
    }
}
