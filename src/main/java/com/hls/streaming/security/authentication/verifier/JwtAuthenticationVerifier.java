package com.hls.streaming.security.authentication.verifier;

import com.hls.streaming.common.exception.ForbiddenException;
import com.hls.streaming.infrastructure.config.error.ErrorCodeConfig;
import com.hls.streaming.infrastructure.config.properties.AuthorizationRuleConfigProperties;
import com.hls.streaming.security.authentication.model.JwtUserDetails;
import com.hls.streaming.security.authentication.model.TokenClaim;
import com.hls.streaming.security.authentication.model.TokenType;
import com.hls.streaming.infrastructure.security.utils.RequestMatcherUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    @Autowired
    public JwtAuthenticationVerifier(final AuthorizationRuleConfigProperties authorizationRuleConfig,
            final ErrorCodeConfig errorCodeConfig) {
        this.strictApiRules = transformRulesConfig(authorizationRuleConfig.getRules());
        this.errorCodeConfig = errorCodeConfig;
    }

    public Authentication verifyAccessPermission(final HttpServletRequest request, final TokenClaim tokenClaim) {

        if (log.isDebugEnabled()) {
            log.debug("Jwt Authentication Verification: request = {} and claim = {}", request.getRequestURI(), tokenClaim);
        }

        if (strictApiRules.containsKey(tokenClaim.getType())
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
