package com.hls.streaming.security.authorization.authorizer;

import com.hls.streaming.security.authentication.model.TokenClaim;
import com.hls.streaming.security.authentication.model.UserRole;
import com.hls.streaming.security.authorization.extractor.HttpMethodExtractor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(value = "io.hls.api-security.enabled")
public class HasAnyRoleAuthorizer implements RequestAuthorizer {

    @Override
    public boolean hasSupported(final String authorizeExpression) {
        return authorizeExpression.startsWith("hasRole")
                || authorizeExpression.startsWith("hasAuthority")
                || authorizeExpression.startsWith("hasAnyRole")
                || authorizeExpression.startsWith("hasAnyAuthority");
    }

    @Override
    public boolean isAccessible(String permissionsExpression, HttpServletRequest request, TokenClaim tokenClaim) {
        final var authorizedRole = UserRole.parseRoles(HttpMethodExtractor.stripAuthorizedExpression(permissionsExpression));
        final var result = tokenClaim.getPrivileges().stream().anyMatch(authorizedRole::contains);
        if (log.isDebugEnabled() && !result) {
            log.debug("Rejected Roles: {}", authorizedRole);
        }
        return result;
    }
}
