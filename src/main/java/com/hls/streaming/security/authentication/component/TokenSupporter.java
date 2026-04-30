package com.hls.streaming.security.authentication.component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hls.streaming.common.constant.ErrorConfigConstants;
import com.hls.streaming.common.exception.BadRequestException;
import com.hls.streaming.common.exception.NotFoundException;
import com.hls.streaming.common.exception.UnauthorizedException;
import com.hls.streaming.infrastructure.config.error.ErrorCodeConfig;
import com.hls.streaming.infrastructure.config.properties.TokenConfigProperties;
import com.hls.streaming.infrastructure.security.utils.ECDSAUtils;
import com.hls.streaming.infrastructure.security.utils.MapUtils;
import com.hls.streaming.security.authentication.dto.GenerateTokenRequest;
import com.hls.streaming.security.authentication.model.FullTokenClaim;
import com.hls.streaming.security.authentication.model.TokenClaim;
import com.hls.streaming.security.authentication.model.TokenType;
import com.hls.streaming.security.authentication.model.UserRole;
import com.hls.streaming.security.authentication.verifier.TokenValidator;
import com.hls.streaming.security.constants.SecurityConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TokenSupporter {

    private static final Base64.Decoder DECODER = Base64.getDecoder();
    public static final String SYSTEM_ID = "000000000000000000000000";

    private final ObjectMapper objectMapper;
    private final TokenConfigProperties tokenConfig;
    private final ErrorCodeConfig errorCodeConfig;

    private final Map<TokenType, TokenConfigProperties.TokenConfig> tokenConfigMap;
    private final Map<String, TokenValidator> tokenVerifiersMap;

    public TokenSupporter(final ObjectMapper objectMapper,
            final TokenConfigProperties tokenConfig,
            final ErrorCodeConfig errorCodeConfig) {
        this.objectMapper = objectMapper;
        this.tokenConfig = tokenConfig;
        this.errorCodeConfig = errorCodeConfig;

        this.tokenConfigMap = CollectionUtils.emptyIfNull(tokenConfig.getTokenConfig())
                .stream()
                .collect(Collectors.toMap(TokenConfigProperties.TokenConfig::getTokenType, Function.identity()));

        final var algorithm = Algorithm.ECDSA512(
                ECDSAUtils.getPublicKey(tokenConfig.getKeyPair().getPublicKey().getBytes(StandardCharsets.UTF_8)),
                ECDSAUtils.getPrivateKey(tokenConfig.getKeyPair().getPrivateKey().getBytes(StandardCharsets.UTF_8)));

        this.tokenVerifiersMap = Map.of("0", TokenValidator.builder()
                .version("0")
                .algorithm(algorithm)
                .verifier(JWT.require(algorithm)
                        .withIssuer(tokenConfig.getIssuer())
                        .build())
                .build());
    }

    public FullTokenClaim verifyAndDecodeToken(final String token) {
        try {
            final var tokenWithoutBearer = token.replace(SecurityConstant.BEARER, "").trim();
            final var tokenVersion = JWT.decode(tokenWithoutBearer).getKeyId();
            final var tokenVerifier = getTokenVerifier(tokenVersion).getVerifier();
            final var jwt = tokenVerifier.verify(tokenWithoutBearer);
            final var tokenClaim = new String(DECODER.decode(jwt.getPayload()), StandardCharsets.UTF_8);
            return objectMapper.readValue(tokenClaim, FullTokenClaim.class);
        } catch (final JsonProcessingException ex) {
            log.error("Parsing token has failed", ex);
            throw new BadRequestException(errorCodeConfig.getMessage(ErrorConfigConstants.PARSING_TOKEN_FAILED), ex);
        }
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
        final var tokenVerifier = getLatestTokenVerifier();
        return JWT.create()
                .withKeyId(tokenVerifier.getVersion())
                .withJWTId(UUID.randomUUID().toString())
                .withPayload(claims)
                .withIssuedAt(now)
                .withIssuer(tokenConfig.getIssuer())
                .withExpiresAt(expiredAt)
                .sign(tokenVerifier.getAlgorithm());
    }

    public String generateSystemToken() {
        final var tokenClaim = TokenClaim.builder()
                .userId(SYSTEM_ID)
                .privileges(Set.of(UserRole.SYSTEM))
                .type(TokenType.ACCESS_TOKEN)
                .build();

        return generateTokenInternal(tokenClaim);
    }

    @SuppressWarnings("unchecked")
    public String generateTokenInternal(final TokenClaim tokenClaim) {
        final var tokenType = tokenClaim.getType();
        final var insNow = Instant.now().minus(1, ChronoUnit.SECONDS);
        final var now = new Date(insNow.toEpochMilli());
        final var livedTimeInMinutes = Optional.ofNullable(tokenConfigMap.get(tokenType))
                .orElseThrow(() -> new UnsupportedOperationException(
                        String.format("Unsupported the TokenType %s", tokenType)))
                .getLivedTime().toMinutes();
        final var expiredAt = new Date(insNow.plus(livedTimeInMinutes, ChronoUnit.MINUTES).toEpochMilli());

        final var userClaims = MapUtils.stripNullValue(objectMapper.convertValue(tokenClaim, Map.class));
        final var claims = Map.of("tokenClaim", userClaims);
        final var tokenVerifier = getLatestTokenVerifier();
        return JWT.create()
                .withKeyId(tokenVerifier.getVersion())
                .withJWTId(createJTI())
                .withPayload(claims)
                .withIssuedAt(now)
                .withIssuer(tokenConfig.getIssuer())
                .withExpiresAt(expiredAt)
                .sign(tokenVerifier.getAlgorithm());
    }

    private String createJTI() {
        return UUID.randomUUID().toString();
    }

    public TokenValidator getLatestTokenVerifier() {
        return tokenVerifiersMap.values().stream()
                .max(Comparator.comparing(verifier -> Integer.valueOf(verifier.getVersion())))
                .orElseThrow(() -> new NotFoundException(
                        errorCodeConfig.getMessage(ErrorConfigConstants.NOT_FOUND_SECRET_VERSION_EMPTY,
                                tokenConfig.getTokenKeypairId())));
    }

    private TokenValidator getTokenVerifier(String version) {
        return Optional.ofNullable(tokenVerifiersMap.get(version))
                .orElseThrow(() -> new UnauthorizedException(
                        errorCodeConfig.getMessage(ErrorConfigConstants.TOKEN_FROM_VERSION_INVALID, version)));
    }
}
