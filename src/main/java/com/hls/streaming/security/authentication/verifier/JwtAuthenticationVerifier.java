package com.hls.streaming.security.authentication.verifier;

import com.hls.streaming.common.exception.ForbiddenException;
import com.hls.streaming.infrastructure.config.error.ErrorCodeConfig;
import com.hls.streaming.infrastructure.config.properties.AuthorizationRuleConfigProperties;
import com.hls.streaming.infrastructure.security.utils.RequestMatcherUtils;
import com.hls.streaming.security.authentication.model.JwtUserDetails;
import com.hls.streaming.security.authentication.model.TokenClaim;
import com.hls.streaming.security.authentication.model.TokenType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.hls.streaming.common.constant.ErrorConfigConstants.ACCESS_FORBIDDEN;
import static java.util.stream.Collectors.groupingBy;

@Slf4j
@Component
public class JwtAuthenticationVerifier {

    private final Map<TokenType, RequestMatcher> strictApiRules;
    private final ErrorCodeConfig errorCodeConfig;
    private final AuthorizationRuleConfigProperties authorizationRuleConfig;

    @Autowired
    public JwtAuthenticationVerifier(final AuthorizationRuleConfigProperties authorizationRuleConfig,
            final ErrorCodeConfig errorCodeConfig, AuthorizationRuleConfigProperties authorizationRuleConfig1) {
        this.strictApiRules = transformRulesConfig(authorizationRuleConfig.getRules());
        this.errorCodeConfig = errorCodeConfig;
        this.authorizationRuleConfig = authorizationRuleConfig1;
    }

    public Authentication verifyAccessPermission(final HttpServletRequest request, final TokenClaim tokenClaim) {

        if (log.isDebugEnabled()) {
            log.debug("Jwt Authentication Verification: request = {} and claim = {}", request.getRequestURI(), tokenClaim);
        }

        if (!isManagementRequestWithAccessToken(request, tokenClaim)
                && strictApiRules.containsKey(tokenClaim.getType())
                && !strictApiRules.get(tokenClaim.getType()).matches(request)) {
            throw new ForbiddenException(errorCodeConfig.getMessage(ACCESS_FORBIDDEN, request.getRequestURI()));
        }

        if (TokenType.REFRESH_TOKEN.equals(tokenClaim.getType())
                && !strictApiRules.containsKey(tokenClaim.getType())) {
            throw new ForbiddenException(errorCodeConfig.getMessage(ACCESS_FORBIDDEN, request.getRequestURI()));
        }

        final var userDetails = new JwtUserDetails(tokenClaim);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private boolean isManagementRequestWithAccessToken(final HttpServletRequest request, final TokenClaim tokenClaim) {

        if (!TokenType.ACCESS_TOKEN.equals(tokenClaim.getType())) {
            return false;
        }

        final var managementApis = authorizationRuleConfig.getManagementApis();

        if (CollectionUtils.isEmpty(managementApis)) {
            return false;
        }

        return managementApis.stream().anyMatch(pattern ->
                new AntPathRequestMatcher(pattern).matches(request)
        );
    }

    public Authentication accessTokenAnonymous(final TokenClaim tokenClaim) {
        final var userDetails = new JwtUserDetails(tokenClaim);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private Map<TokenType, RequestMatcher> transformRulesConfig(
            final List<AuthorizationRuleConfigProperties.AuthorizationRuleConfig> strictApiConfigList) {

        final Map<TokenType, Set<String>> ruleMap = strictApiConfigList.stream()
                .collect(
                        groupingBy(AuthorizationRuleConfigProperties.AuthorizationRuleConfig::getToken,
                                Collector.of(
                                        HashSet::new,
                                        (set, entry) -> set.addAll(entry.getPaths()),
                                        (set1, set2) -> {
                                            set1.addAll(set2);
                                            return set1;
                                        })));

        return ruleMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> RequestMatcherUtils.createRequestMatcherWithOrPatternByPaths(entry.getValue())));
    }
}
