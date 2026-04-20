package com.hls.streaming.config.properties;

import com.hls.streaming.security.models.TokenType;
import com.hls.streaming.security.models.UserRole;
import com.hls.streaming.security.utils.UserRoleUtils;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@ConfigurationProperties(prefix = "io.hls.api-security.authorization-rules")
public class AuthorizationRuleConfigProperties {

    private AuthorizationRuleConfig defaultRule;
    private SkippedAuthorizationRuleConfig skippedAuthorization;
    private List<AuthorizationRuleConfig> rules;
    private boolean authorizeAll = true;
    private String urlPrefix;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorizationRuleConfig {
        @Getter
        @Setter
        private TokenType token;

        @Setter
        private String roles;

        @Getter
        @Setter
        private Set<String> paths;

        public Set<UserRole> getRoles() {
            return UserRoleUtils.parseRoles(roles);
        }
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedAuthorizationRuleConfig {
        @Setter
        private Set<String> systemApis;

        @Setter
        private Set<String> publicApis;

        public Set<String> getSkippedApis() {
            return Stream.concat(systemApis.stream(), publicApis.stream()).collect(Collectors.toSet());
        }
    }
}
