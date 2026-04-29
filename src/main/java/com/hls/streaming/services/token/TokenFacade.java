package com.hls.streaming.services.token;

import com.hls.streaming.documents.user.User;
import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.features.mapper.token.TokenMapperFacade;
import com.hls.streaming.security.models.TokenType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenFacade {

    private final TokenGenerator tokenGenerator;
    private final TokenMapperFacade tokenMapperFacade;

    public UserAccessResponse generateAccessTokenPair(final User user) {

        var roles = user.getRoles();

        var accessToken = tokenGenerator.generate(user, TokenType.ACCESS_TOKEN, roles);
        var refreshToken = tokenGenerator.generate(user, TokenType.REFRESH_TOKEN, roles);

        return tokenMapperFacade.toAccessResponse(user, accessToken, refreshToken);
    }

    public String generateToken(final User user, final TokenType tokenType) {
        return tokenGenerator.generateWithDefaultRole(user, tokenType);
    }
}
