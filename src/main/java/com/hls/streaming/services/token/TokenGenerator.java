package com.hls.streaming.services.token;

import com.hls.streaming.documents.user.User;
import com.hls.streaming.dtos.token.GenerateTokenRequest;
import com.hls.streaming.security.component.TokenSupporter;
import com.hls.streaming.security.models.TokenType;
import com.hls.streaming.security.models.UserRole;
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
