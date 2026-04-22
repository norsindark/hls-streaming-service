package com.hls.streaming.security.component;

import com.hls.streaming.exception.AuthorizationException;
import com.hls.streaming.config.error.ErrorCodeConfig;
import com.hls.streaming.security.constants.SecurityConstant;
import com.hls.streaming.security.context.AppSecurityContextHolder;
import com.hls.streaming.security.context.HttpSecurityContext;
import com.hls.streaming.security.models.TokenClaim;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.hls.streaming.constant.ErrorConfigConstants.AUTHORIZATION_MUST;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenClaimExtractor {

    private final TokenSupporter tokenVerifier;
    private final ErrorCodeConfig errorCodeConfig;

    @Value("${io.hls.websocket.endpoints:}")
    private List<String> wsEndpoints;

    @Value("${server.servlet.context-path:}")
    private String servletContextPath;

    public TokenClaim fromAuthorizationToken(final HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("Extracting token in: {}", request.getRequestURI());
        }

        final var tokenAsString = extractToken(request);
        final var fullTokenClaim = tokenVerifier.verifyAndDecodeToken(tokenAsString);

        AppSecurityContextHolder.setContext(HttpSecurityContext.httpSecurityContextBuilder()
                .authorizationToken(tokenAsString)
                .fullTokenClaim(fullTokenClaim)
                .build());

        return fullTokenClaim.getTokenClaim();
    }

    public String extractToken(final HttpServletRequest request) {

        var tempRequestURI = request.getRequestURI();
        if (StringUtils.isNotBlank(tempRequestURI)) {
            tempRequestURI = request.getRequestURI().replaceFirst(servletContextPath, "");
        }

        if (wsEndpoints.contains(tempRequestURI)) {
            final String token = request.getParameter(SecurityConstant.ACCESS_TOKEN);
            if (log.isDebugEnabled()) {
                log.debug(String.format("WebSocket Authentication: Access token parameter check: %s", token));
            }
            if (token != null) {
                return token;
            }
        }

        var token = Optional.ofNullable(request.getHeader(SecurityConstant.AUTHORIZATION))
                .orElseThrow(() -> new AuthorizationException(errorCodeConfig.getMessage(AUTHORIZATION_MUST)));
        return token.replace("Apikey ", "");
    }
}
