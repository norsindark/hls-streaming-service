package com.hls.streaming.security.authentication.service;

import com.hls.streaming.user.domain.document.User;
import com.hls.streaming.security.authentication.dto.GenerateTokenRequest;
import com.hls.streaming.security.authentication.component.TokenSupporter;
import com.hls.streaming.security.authentication.model.TokenType;
import com.hls.streaming.security.authentication.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class TokenGenerator {

    private final TokenSupporter tokenSupporter;

    public String generate(final User user, final TokenType tokenType, final Set<UserRole> roles) {

        var request = GenerateTokenRequest.builder()
                .userId(user.getId())
                .tokenType(tokenType)
                .build();

        return tokenSupporter.generateToken(request, roles);
    }

    public String generateWithDefaultRole(final User user, final TokenType tokenType) {
        return generate(user, tokenType, Set.of(UserRole.USER));
    }
}
