package com.hls.streaming.services.token;

import com.hls.streaming.documents.user.User;
import com.hls.streaming.dtos.token.GenerateTokenRequest;
import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.dtos.user.UserLiteResponse;
import com.hls.streaming.enums.UserFlowStatusEnum;
import com.hls.streaming.enums.UserStatusEnum;
import com.hls.streaming.security.component.TokenSupporter;
import com.hls.streaming.security.models.TokenType;
import com.hls.streaming.security.models.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenSupporter tokenSupporter;

    public UserAccessResponse generateAccessTokenPair(final User user) {
        final var userId = user.getId();
        final var userRoles = user.getRoles();

        final var accessTokenRequest = GenerateTokenRequest.builder()
                .userId(userId)
                .tokenType(TokenType.ACCESS_TOKEN)
                .build();

        final var refreshTokenRequest = GenerateTokenRequest.builder()
                .userId(userId)
                .tokenType(TokenType.REFRESH_TOKEN)
                .build();

        final var accessToken = tokenSupporter.generateToken(accessTokenRequest, userRoles);
        final var refreshToken = tokenSupporter.generateToken(refreshTokenRequest, userRoles);

        return UserAccessResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .status(UserFlowStatusEnum.COMPLETED)
                .userInfo(UserLiteResponse.builder()
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .displayName(user.getDisplayName())
                        .isVerified(UserStatusEnum.ACTIVE.equals(user.getStatus()))
                        .isAdmin(userRoles.contains(UserRole.ADMIN))
                        .build())
                .build();
    }

    public String generateToken(final User user, final TokenType tokenType) {
        final var userRoles = Set.of(UserRole.USER);

        final var tokenRequest = GenerateTokenRequest.builder()
                .userId(user.getId())
                .tokenType(tokenType)
                .build();

        return tokenSupporter.generateToken(tokenRequest, userRoles);
    }
}
