package com.hls.streaming.services.user;

import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.dtos.user.IdentifyUserRequest;
import com.hls.streaming.dtos.user.UserLiteResponse;
import com.hls.streaming.dtos.user.VerifyPasswordRequest;
import com.hls.streaming.enums.UserFlowStatusEnum;
import com.hls.streaming.enums.UserStatusEnum;
import com.hls.streaming.exception.BadRequestException;
import com.hls.streaming.exception.NotFoundException;
import com.hls.streaming.repositories.user.UserRepository;
import com.hls.streaming.security.models.TokenType;
import com.hls.streaming.services.token.TokenService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public UserAccessResponse identifyUser(final IdentifyUserRequest request) {
        if (StringUtils.isBlank(request.getIdentifier())) {
            throw new BadRequestException("Identifier cannot be empty");
        }

        final var userDocument = userRepository.findByEmail(request.getIdentifier())
                .orElseGet(() -> userRepository.findByUsername(request.getIdentifier())
                        .orElseThrow(() -> new NotFoundException("User not found")));

        if (!UserStatusEnum.ACTIVE.equals(userDocument.getStatus())) {
            throw new BadRequestException("User is currently inactive");
        }

        final var passwordVerificationToken = tokenService.generateToken(userDocument, TokenType.PASSWORD_VERIFICATION_TOKEN);

        return UserAccessResponse.builder()
                .userInfo(UserLiteResponse.builder()
                        .id(userDocument.getId())
                        .username(userDocument.getUsername())
                        .displayName(userDocument.getDisplayName())
                        .avatar(userDocument.getAvatar())
                        .build())
                .status(UserFlowStatusEnum.WAITING_FOR_VERIFICATION)
                .passwordVerificationToken(passwordVerificationToken)
                .build();
    }

    public UserAccessResponse verifyPassword(final VerifyPasswordRequest request) {
        final var userDocument = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), userDocument.getPassword())) {

            throw new BadRequestException("Invalid password");
        }

        return tokenService.generateAccessTokenPair(userDocument);
    }
}
