package com.hls.streaming.security.jwt;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hls.streaming.config.error.ErrorCodeConfig;
import com.hls.streaming.config.properties.AuthorizationRuleConfigProperties;
import com.hls.streaming.constant.ErrorConfigConstants;
import com.hls.streaming.exception.*;
import com.hls.streaming.security.component.TokenClaimExtractor;
import com.hls.streaming.security.constants.SecurityConstant;
import com.hls.streaming.security.context.AppSecurityContextHolder;
import com.hls.streaming.security.models.TokenClaim;
import com.hls.streaming.security.utils.RequestMatcherUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Map;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RequestMatcher ignoreRequests;
    private final TokenClaimExtractor tokenClaimExtractor;
    private final JwtAuthenticationVerifier authenticationVerifier;
    private final ErrorCodeConfig errorCodeConfig;
    private final boolean authorizeAll;
    private final String urlPrefix;

    public JwtAuthenticationFilter(
            AuthorizationRuleConfigProperties authorizationRuleConfig,
            TokenClaimExtractor tokenClaimExtractor,
            JwtAuthenticationVerifier authenticationVerifier,
            ErrorCodeConfig errorCodeConfig) {
        this.ignoreRequests = RequestMatcherUtils.createRequestMatcherWithOrPatternByPaths(
                authorizationRuleConfig.getSkippedAuthorization().getSkippedApis());
        this.tokenClaimExtractor = tokenClaimExtractor;
        this.authenticationVerifier = authenticationVerifier;
        this.errorCodeConfig = errorCodeConfig;
        this.authorizeAll = authorizationRuleConfig.isAuthorizeAll();
        this.urlPrefix = authorizationRuleConfig.getUrlPrefix();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) {

        try {

            if (!isRequiredAuthentication(request)) {
                if (request.getHeader(SecurityConstant.AUTHORIZATION) != null) {
                    TokenClaim tokenClaim = parseToken(request);
                    var authentication = authenticationVerifier.accessTokenAnonymous(tokenClaim);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

                filterChain.doFilter(request, response);
                return;
            }

            if (authorizeAll || (StringUtils.isNotBlank(urlPrefix) && request.getRequestURI().startsWith(urlPrefix))) {
                TokenClaim tokenClaim = parseToken(request);
                var authentication = authenticationVerifier.verifyAccessPermission(request, tokenClaim);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);

        } catch (TokenExpiredException ex) {
            log.warn("Token expired");
            writeError(response, ex);
        } catch (AuthorizationException ex) {
            log.warn("Authorization missing");
            writeError(response, ex);
        } catch (JWTDecodeException ex) {
            log.warn("JWT decode error");
            writeError(response, new UnauthorizedException(errorCodeConfig.getMessage(ErrorConfigConstants.TOKEN_INVALID)));
        } catch (ForbiddenException ex) {
            log.warn("Forbidden");
            writeError(response, ex);
        } catch (BadRequestException ex) {
            log.warn("Bad Request");
            writeError(response, ex);
        } catch (Exception ex) {
            writeError(response,
                    new InternalServerErrorException(errorCodeConfig.getMessage(), ex));
        } finally {
            SecurityContextHolder.clearContext();
            AppSecurityContextHolder.clearContext();
        }
    }

    private TokenClaim parseToken(HttpServletRequest request) {
        try {
            return tokenClaimExtractor.fromAuthorizationToken(request);
        } catch (com.auth0.jwt.exceptions.TokenExpiredException e) {
            throw new TokenExpiredException(
                    errorCodeConfig.getMessage(ErrorConfigConstants.TOKEN_EXPIRED));
        }
    }

    private boolean isRequiredAuthentication(HttpServletRequest request) {
        return !ignoreRequests.matches(request);
    }

    private void writeError(HttpServletResponse response, RuntimeException ex) {
        try {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new ObjectMapper()
                            .writeValueAsString(Map.of("error", ex.getMessage())));
        } catch (Exception ignored) {}
    }
}
