package com.hls.streaming.services.token.generator;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hls.streaming.config.properties.TokenConfigProperties;
import com.hls.streaming.dtos.token.GenerateTokenRequest;
import com.hls.streaming.security.component.TokenSupporter;
import com.hls.streaming.security.models.TokenClaim;
import com.hls.streaming.security.models.TokenType;
import com.hls.streaming.security.models.TokenVerifier;
import com.hls.streaming.security.models.UserRole;
import com.hls.streaming.security.utils.MapUtils;
import com.hls.streaming.utils.NanoIdGenerator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@DependsOn("tokenSupporter")
@RequiredArgsConstructor
public class UserTokenGenerator {

    private final ObjectMapper objectMapper;
    private final TokenConfigProperties tokenConfig;
    private final TokenSupporter tokenSupporter;

    private Map<TokenType, TokenConfigProperties.TokenConfig> tokenConfigMap;
    private TokenVerifier tokenVerifier;

    @PostConstruct
    public void init() {
        this.tokenVerifier = tokenSupporter.getLatestTokenVerifier();
        this.tokenConfigMap = CollectionUtils.emptyIfNull(tokenConfig.getTokenConfig())
                .stream()
                .collect(Collectors.toMap(TokenConfigProperties.TokenConfig::getTokenType, Function.identity()));
    }

    private static String createJTI() {
        return NanoIdGenerator.randomId();
    }

    @SuppressWarnings("unchecked")
    public String generateToken(final GenerateTokenRequest request, final Set<UserRole> roles) {
        final var insNow = Instant.now();
        final var now = new Date(insNow.toEpochMilli());

        final var livedTimeInMinutes = Optional.ofNullable(tokenConfigMap.get(request.getTokenType()))
                .orElseThrow(() -> new UnsupportedOperationException(
                        String.format("Unsupported the TokenType %s", request.getTokenType())))
                .getLivedTime()
                .toMinutes();

        final var expiredAt = new Date(insNow.plus(livedTimeInMinutes, ChronoUnit.MINUTES).toEpochMilli());

        final var tokenClaim = TokenClaim.builder()
                .userId(request.getUserId())
                .privileges(roles)
                .type(request.getTokenType())
                .build();

        final var userClaims = MapUtils.stripNullValue(objectMapper.convertValue(tokenClaim, Map.class));
        final var claims = Map.of("tokenClaim", userClaims);

        return JWT.create()
                .withKeyId(tokenVerifier.getVersion())
                .withJWTId(createJTI())
                .withPayload(claims)
                .withIssuedAt(now)
                .withIssuer(tokenConfig.getIssuer())
                .withExpiresAt(expiredAt)
                .sign(tokenVerifier.getAlgorithm());
    }
}
