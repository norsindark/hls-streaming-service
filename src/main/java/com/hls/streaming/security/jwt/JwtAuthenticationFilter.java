package com.hls.streaming.security.jwt;

import com.auth0.jwt.exceptions.JWTDecodeException;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RequestMatcher ignoreRequests;
    private final TokenClaimExtractor tokenClaimExtractor;
    private final JwtAuthenticationVerifier authenticationVerifier;
    private final ErrorCodeConfig errorCodeConfig;
    private final boolean authorizeAll;
    private final String urlPrefix;
    private final HandlerExceptionResolver exceptionResolver;

    public JwtAuthenticationFilter(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
            AuthorizationRuleConfigProperties authorizationRuleConfig,
            TokenClaimExtractor tokenClaimExtractor,
            JwtAuthenticationVerifier authenticationVerifier,
            ErrorCodeConfig errorCodeConfig) {
        this.exceptionResolver = exceptionResolver;
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

        final TokenClaim tokenClaim;
        try {

            if (!isRequiredAuthentication(request)) {
                if (!request.getRequestURI().contains("/management/health")) {
                    log.info("Skipping authentication request = {}", request.getRequestURI());
                }

                if (request.getHeader(SecurityConstant.AUTHORIZATION) != null) {
                    tokenClaim = parseToken(request);
                    final var authentication = authenticationVerifier.accessTokenAnonymous(tokenClaim);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                filterChain.doFilter(request, response);
                return;
            }

            if (authorizeAll) {
                log.info("Attempting Authentication: request = {}", request.getRequestURI());
                tokenClaim = parseToken(request);
                final var authentication = authenticationVerifier.verifyAccessPermission(request, tokenClaim);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                if (StringUtils.isNotBlank(urlPrefix) && request.getRequestURI().startsWith(urlPrefix)) {
                    log.info("Attempting Authentication: request = {}", request.getRequestURI());
                    tokenClaim = parseToken(request);
                    final var authentication = authenticationVerifier.verifyAccessPermission(request, tokenClaim);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            filterChain.doFilter(request, response);
        } catch (final TokenExpiredException ex) {
            log.warn("Token has been expired.");
            exceptionResolver.resolveException(request, response, null, ex);
        } catch (final AuthorizationException ex) {
            log.warn("Token is missing.");
            exceptionResolver.resolveException(request, response, null, ex);
        } catch (final JWTDecodeException ex) {
            log.warn("Token Decoding has error.");
            exceptionResolver.resolveException(request, response, null,
                    new UnauthorizedException(errorCodeConfig.getMessage(ErrorConfigConstants.TOKEN_INVALID)));
        } catch (final ForbiddenException ex) {
            log.warn("Forbidden API Accessing.");
            exceptionResolver.resolveException(request, response, null, ex);
        } catch (final BadRequestException ex) {
            log.warn("Bad Request Error.");
            exceptionResolver.resolveException(request, response, null, ex);
        } catch (final Exception ex) {
            log.warn("Unexpected Error.");
            ex.printStackTrace();
            exceptionResolver.resolveException(request, response, null,
                    new InternalServerErrorException(errorCodeConfig.getMessage(), ex.getCause()));
        } finally {
            clearLocalThreadData();
        }
    }

    private void clearLocalThreadData() {
        SecurityContextHolder.clearContext();
        AppSecurityContextHolder.clearContext();
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

}
