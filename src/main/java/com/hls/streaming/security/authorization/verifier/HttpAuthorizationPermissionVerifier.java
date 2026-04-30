package com.hls.streaming.security.authorization.verifier;

import com.hls.streaming.security.authorization.extractor.HttpMethodExtractor;
import com.hls.streaming.infrastructure.config.error.ErrorCodeConfig;
import com.hls.streaming.common.exception.ForbiddenException;
import com.hls.streaming.security.authentication.model.HttpRequestSecurity;
import com.hls.streaming.security.authentication.model.JwtUserDetails;
import com.hls.streaming.security.authorization.authorizer.RequestAuthorizer;
import com.hls.streaming.infrastructure.security.utils.RequestMatcherUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static com.hls.streaming.common.constant.ErrorConfigConstants.ACCESS_FORBIDDEN;

@Slf4j
@Component
@ConditionalOnProperty(value = "io.hls.api-security.enabled")
@RequiredArgsConstructor
public class HttpAuthorizationPermissionVerifier {

    private final ApplicationContext applicationContext;
    private final List<RequestAuthorizer> requestAuthorizers;
    private final ErrorCodeConfig errorCodeConfig;

    private List<HttpRequestSecurity> httpAuthorizationsRegistry;

    @Getter
    private RequestMatcher authorizedRequestMatcher;

    @PostConstruct
    public void initialize() {

        this.httpAuthorizationsRegistry =
                applicationContext.getBeansWithAnnotation(RestController.class)
                        .values()
                        .stream()
                        .filter(bean -> ClassUtils.getPackageName(
                                AopUtils.getTargetClass(bean)).startsWith("com.hls"))
                        .flatMap(bean -> extractAllMethods(bean).stream())
                        .map(this::buildHttpRequestSecurity)
                        .filter(Objects::nonNull)
                        .toList();

        this.authorizedRequestMatcher = httpAuthorizationsRegistry.isEmpty()
                ? RequestMatcherUtils.createPermitAll()
                : RequestMatcherUtils.createRequestMatcherWithOrPattern(
                        httpAuthorizationsRegistry.stream()
                                .map(HttpRequestSecurity::getHttpRequest)
                                .toList());
    }

    private Set<Method> extractAllMethods(Object bean) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);

        var methods = new HashSet<>(Arrays.asList(targetClass.getMethods()));

        for (Class<?> iface : targetClass.getInterfaces()) {
            methods.addAll(Arrays.asList(iface.getMethods()));
        }

        return methods.stream()
                .map(method -> AopUtils.getMostSpecificMethod(method, targetClass))
                .collect(Collectors.toSet());
    }

    private HttpRequestSecurity buildHttpRequestSecurity(Method method) {

        return HttpMethodExtractor.getHttpMappingAnnotation(method)
                .map(request -> {
                    var preAuthorizeExpression =
                            AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class) != null
                                    ? HttpMethodExtractor.getPreAuthorizeExpression(method)
                                    : null;

                    if (Objects.isNull(preAuthorizeExpression)) {
                        return null;
                    }

                    final var expression = preAuthorizeExpression.trim();

                    final var authorizers = requestAuthorizers.stream()
                            .filter(it -> it.hasSupported(expression))
                            .toList();

                    if (authorizers.isEmpty()) {
                        throw new IllegalStateException(String.format(
                                "Unsupported Authorization Expression: [%s]", expression));
                    }

                    return HttpRequestSecurity.builder()
                            .httpRequest(request)
                            .requestMatcher(RequestMatcherUtils.createRequestMatcher(request))
                            .permissionsExpression(expression)
                            .authorizers(authorizers)
                            .build();
                })
                .orElse(null);
    }

    public void authorizeAccessPermission(final Authentication authentication,
            final HttpServletRequest request) {

        if (!authorizedRequestMatcher.matches(request)) {
            log.info("Skipping Authorization since the request is not required.");
            return;
        }

        final var httpAuthorization = lookupHttpAuthorization(request);

        final var jwtUserDetails = (JwtUserDetails) authentication.getPrincipal();

        if (!httpAuthorization.getAuthorizers()
                .stream()
                .allMatch(authorizer ->
                        authorizer.isAccessible(
                                httpAuthorization.getPermissionsExpression(),
                                request,
                                jwtUserDetails.getTokenClaim()))) {

            throw new ForbiddenException(
                    errorCodeConfig.getMessage(ACCESS_FORBIDDEN, request.getRequestURI()));
        }
    }

    private HttpRequestSecurity lookupHttpAuthorization(final HttpServletRequest request) {

        final var httpAuthorizations = httpAuthorizationsRegistry.stream()
                .filter(it -> it.getRequestMatcher().matches(request))
                .toList();

        if (httpAuthorizations.isEmpty()) {
            throw notFoundAuthorizationRequestException(request.getRequestURI());
        } else if (httpAuthorizations.size() == 1) {
            return httpAuthorizations.getFirst();
        }

        return httpAuthorizations.stream()
                .filter(it -> it.getHttpRequest().getPath()
                        .equals(request.getServletPath()))
                .findFirst()
                .orElseThrow(() ->
                        notFoundAuthorizationRequestException(request.getRequestURI()));
    }

    private IllegalArgumentException notFoundAuthorizationRequestException(final String uri) {
        return new IllegalArgumentException(
                String.format("Not Found Authorization Request Security with [%s]", uri));
    }
}
