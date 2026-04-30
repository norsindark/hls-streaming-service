package com.hls.streaming.security.authentication.service;

import com.hls.streaming.security.authentication.dto.UserAccessResponse;
import com.hls.streaming.security.authentication.mapper.TokenMapper;
import com.hls.streaming.security.authentication.model.TokenType;
import com.hls.streaming.user.domain.document.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenFacade {

    private final TokenGenerator tokenGenerator;
    private final TokenMapper tokenMapper;

    public UserAccessResponse generateAccessTokenPair(final User user) {

        var roles = user.getRoles();

        var accessToken = tokenGenerator.generate(user, TokenType.ACCESS_TOKEN, roles);
        var refreshToken = tokenGenerator.generate(user, TokenType.REFRESH_TOKEN, roles);

        return tokenMapper.toAccessResponse(user, accessToken, refreshToken);
    }

    public String generateToken(final User user, final TokenType tokenType) {
        return tokenGenerator.generateWithDefaultRole(user, tokenType);
    }
}
