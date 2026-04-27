package com.hls.streaming.services.user;

import com.hls.streaming.config.error.ErrorCodeConfig;
import com.hls.streaming.constant.ErrorConfigConstants;
import com.hls.streaming.documents.user.User;
import com.hls.streaming.documents.user.UserDetail;
import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.dtos.user.IdentifyUserRequest;
import com.hls.streaming.dtos.user.RegisterUserRequest;
import com.hls.streaming.dtos.user.UserLiteResponse;
import com.hls.streaming.dtos.user.VerifyPasswordRequest;
import com.hls.streaming.enums.UserFlowStatusEnum;
import com.hls.streaming.enums.UserStatusEnum;
import com.hls.streaming.exception.BadRequestException;
import com.hls.streaming.exception.NotFoundException;
import com.hls.streaming.repositories.user.UserRepository;
import com.hls.streaming.security.models.TokenType;
import com.hls.streaming.security.models.UserRole;
import com.hls.streaming.services.token.TokenService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final ErrorCodeConfig errorCodeConfig;

    @Transactional(rollbackFor = Exception.class)
    public UserAccessResponse register(final RegisterUserRequest request) {
        if (StringUtils.isBlank(request.getUsername()) || StringUtils.isBlank(request.getEmail())
                || StringUtils.isBlank(request.getPassword())) {
            throw new BadRequestException("Required fields cannot be empty");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException(errorCodeConfig.getMessage(ErrorConfigConstants.USERNAME_ALREADY_EXISTS));
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException(errorCodeConfig.getMessage(ErrorConfigConstants.EMAIL_ALREADY_EXISTS));
        }

        final var userDetail = UserDetail.builder()
                .enableNotify(true)
                .build();

        final var user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .displayName(StringUtils.isNotBlank(request.getDisplayName()) ? request.getDisplayName() : request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.USER))
                .detail(userDetail)
                .build();

        final var savedUser = userRepository.save(user);

        return tokenService.generateAccessTokenPair(savedUser);
    }

    public UserAccessResponse identifyUser(final IdentifyUserRequest request) {
        if (StringUtils.isBlank(request.getIdentifier())) {
            throw new BadRequestException(errorCodeConfig.getMessage(ErrorConfigConstants.IDENTIFIER_EMPTY));
        }

        final var userDocument = userRepository.findByEmail(request.getIdentifier())
                .orElseGet(() -> userRepository.findByUsername(request.getIdentifier())
                        .orElseThrow(() -> new NotFoundException(errorCodeConfig.getMessage(ErrorConfigConstants.USER_NOT_FOUND))));

        if (!UserStatusEnum.ACTIVE.equals(userDocument.getStatus())) {
            throw new BadRequestException(errorCodeConfig.getMessage(ErrorConfigConstants.USER_INACTIVE));
        }

        final var passwordVerificationToken = tokenService.generateToken(userDocument, TokenType.PASSWORD_VERIFICATION_TOKEN);

        return UserAccessResponse.builder()
                .userInfo(UserLiteResponse.builder()
                        .id(userDocument.getId())
                        .username(userDocument.getUsername())
                        .email(userDocument.getEmail())
                        .displayName(userDocument.getDisplayName())
                        .avatar(userDocument.getAvatar())
                        .build())
                .status(UserFlowStatusEnum.WAITING_FOR_VERIFICATION)
                .passwordVerificationToken(passwordVerificationToken)
                .build();
    }

    public UserAccessResponse verifyPassword(final VerifyPasswordRequest request) {
        final var userDocument = userRepository.findById(new ObjectId(request.getUserId()))
                .orElseThrow(() -> new NotFoundException(errorCodeConfig.getMessage(ErrorConfigConstants.USER_NOT_FOUND, request.getUserId())));

        if (!UserStatusEnum.ACTIVE.equals(userDocument.getStatus())) {
            throw new BadRequestException(errorCodeConfig.getMessage(ErrorConfigConstants.USER_INACTIVE));
        }

        if (!passwordEncoder.matches(request.getPassword(), userDocument.getPassword())) {
            throw new BadRequestException(errorCodeConfig.getMessage(ErrorConfigConstants.INVALID_PASSWORD));
        }

        return tokenService.generateAccessTokenPair(userDocument);
    }

    public UserLiteResponse getProfile(final String userId) {
        final var userDocument = userRepository.findById(new ObjectId(userId))
                .orElseThrow(() -> new NotFoundException(errorCodeConfig.getMessage(ErrorConfigConstants.USER_NOT_FOUND, userId)));

        return UserLiteResponse.builder()
                .id(userDocument.getId())
                .username(userDocument.getUsername())
                .email(userDocument.getEmail())
                .displayName(userDocument.getDisplayName())
                .avatar(userDocument.getAvatar())
                .isActive(UserStatusEnum.ACTIVE.equals(userDocument.getStatus()))
                .isAdmin(userDocument.getRoles().contains(UserRole.ADMIN))
                .build();
    }
}
