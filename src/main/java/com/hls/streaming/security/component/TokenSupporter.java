package com.hls.streaming.security.component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hls.streaming.config.error.ErrorCodeConfig;
import com.hls.streaming.config.properties.TokenConfigProperties;
import com.hls.streaming.constant.ErrorConfigConstants;
import com.hls.streaming.exception.BadRequestException;
import com.hls.streaming.exception.NotFoundException;
import com.hls.streaming.exception.UnauthorizedException;
import com.hls.streaming.security.constants.SecurityConstant;
import com.hls.streaming.security.models.*;
import com.hls.streaming.security.utils.ECDSAUtils;
import com.hls.streaming.security.utils.MapUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final Map<String, TokenVerifier> tokenVerifiersMap;

    @Autowired
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

        this.tokenVerifiersMap = Map.of("0", TokenVerifier.builder()
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

    public String generateSystemToken() {
        final var tokenClaim = TokenClaim.builder()
                .userId(SYSTEM_ID)
                .privileges(Set.of(UserRole.SYSTEM))
                .type(TokenType.ACCESS_TOKEN)
                .build();

        return generateToken(tokenClaim);
    }

    @SuppressWarnings("unchecked")
    public String generateToken(final TokenClaim tokenClaim) {
        final var insNow = Instant.now().minus(1, ChronoUnit.SECONDS);
        final var now = new Date(insNow.toEpochMilli());
        final var livedTimeInMinutes =
                Optional.ofNullable(tokenConfigMap.get(TokenType.ACCESS_TOKEN))
                        .orElseThrow(() -> new UnsupportedOperationException(
                                String.format("Unsupported the TokenType %s", TokenType.ACCESS_TOKEN)))
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

    public TokenVerifier getLatestTokenVerifier() {
        return tokenVerifiersMap.values().stream()
                .max(Comparator.comparing(verifier -> Integer.valueOf(verifier.getVersion())))
                .orElseThrow(() -> new NotFoundException(
                        errorCodeConfig.getMessage(ErrorConfigConstants.NOT_FOUND_SECRET_VERSION_EMPTY,
                                tokenConfig.getTokenKeypairId())));
    }

    private TokenVerifier getTokenVerifier(String version) {
        return Optional.ofNullable(tokenVerifiersMap.get(version))
                .orElseThrow(() -> new UnauthorizedException(
                        errorCodeConfig.getMessage(ErrorConfigConstants.TOKEN_FROM_VERSION_INVALID, version)));
    }

}
