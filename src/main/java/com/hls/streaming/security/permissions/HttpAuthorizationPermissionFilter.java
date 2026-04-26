package com.hls.streaming.security.permissions;


import com.hls.streaming.config.error.ErrorCodeConfig;
import com.hls.streaming.config.properties.AuthorizationRuleConfigProperties;
import com.hls.streaming.exception.UnauthorizedException;
import com.hls.streaming.security.utils.RequestMatcherUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

import static com.hls.streaming.constant.ErrorConfigConstants.AUTHORIZATION_MUST;

@Slf4j
public class HttpAuthorizationPermissionFilter extends OncePerRequestFilter {

    private final HttpAuthorizationPermissionVerifier httpAuthorizationPermissionVerifier;
    private final ErrorCodeConfig errorCodeConfig;
    private final RequestMatcher ignoreRequests;
    private final boolean authorizeAll;
    private final String urlPrefix;

    public HttpAuthorizationPermissionFilter(final ErrorCodeConfig errorCodeConfig,
            final AuthorizationRuleConfigProperties authorizationRuleConfig,
            final HttpAuthorizationPermissionVerifier httpAuthorizationPermissionVerifier) {

        this.httpAuthorizationPermissionVerifier = httpAuthorizationPermissionVerifier;
        this.errorCodeConfig = errorCodeConfig;
        this.ignoreRequests = RequestMatcherUtils.createRequestMatcherWithOrPatternByPaths(
                authorizationRuleConfig.getSkippedAuthorization().getSkippedApis());
        this.authorizeAll = authorizationRuleConfig.isAuthorizeAll();
        this.urlPrefix = authorizationRuleConfig.getUrlPrefix();
    }

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain filterChain)
            throws ServletException, IOException {

        if (!isRequiredAuthentication(request)) {
            if (log.isDebugEnabled()) {
                log.debug("Skipping http authorization permission request = {}", request.getRequestURI());
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Attempting authorization permission request = {}", request.getRequestURI());
        }

        if (authorizeAll) {
            final var authentication = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                    .orElseThrow(() -> new UnauthorizedException(errorCodeConfig.getMessage(AUTHORIZATION_MUST)));
            httpAuthorizationPermissionVerifier.authorizeAccessPermission(authentication, request);
        } else {
            if (StringUtils.isNotBlank(urlPrefix) && request.getRequestURI().startsWith(urlPrefix)) {
                final var authentication = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                        .orElseThrow(() -> new UnauthorizedException(errorCodeConfig.getMessage(AUTHORIZATION_MUST)));
                httpAuthorizationPermissionVerifier.authorizeAccessPermission(authentication, request);
            }
        }

        filterChain.doFilter(request, response);
    }

    protected boolean isRequiredAuthentication(final HttpServletRequest request) {
        return !ignoreRequests.matches(request)
                && httpAuthorizationPermissionVerifier.getAuthorizedRequestMatcher().matches(request);
    }
}
