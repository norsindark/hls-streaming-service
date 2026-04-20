package com.hls.streaming.services.token;

import com.hls.streaming.documents.user.UserDocument;
import com.hls.streaming.dtos.token.GenerateTokenRequest;
import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.enums.UserFlowStatusEnum;
import com.hls.streaming.security.models.TokenType;
import com.hls.streaming.security.models.UserRole;
import com.hls.streaming.services.token.generator.UserTokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final UserTokenGenerator tokenGenerator;

    public UserAccessResponse generateAccessTokenPair(final UserDocument userDocument) {
        final var userId = userDocument.getId();
        final var userRoles = Set.of(UserRole.USER);

        final var accessTokenRequest = GenerateTokenRequest.builder()
                .userId(userId)
                .tokenType(TokenType.ACCESS_TOKEN)
                .build();

        final var refreshTokenRequest = GenerateTokenRequest.builder()
                .userId(userId)
                .tokenType(TokenType.REFRESH_TOKEN)
                .build();

        final var accessToken = tokenGenerator.generateToken(accessTokenRequest, userRoles);
        final var refreshToken = tokenGenerator.generateToken(refreshTokenRequest, userRoles);

        return UserAccessResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .status(UserFlowStatusEnum.COMPLETED)
                .build();
    }

    public String generateToken(final UserDocument userDocument, final TokenType tokenType) {
        final var userRoles = Set.of(UserRole.USER);

        final var tokenRequest = GenerateTokenRequest.builder()
                .userId(userDocument.getId())
                .tokenType(tokenType)
                .build();

        return tokenGenerator.generateToken(tokenRequest, userRoles);
    }
}
