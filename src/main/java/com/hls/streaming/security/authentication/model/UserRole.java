package com.hls.streaming.security.authentication.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hls.streaming.security.constants.SecurityConstant;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum UserRole {

    ADMIN(SecurityConstant.UserRole.ADMIN), SYSTEM(SecurityConstant.UserRole.SYSTEM), USER(SecurityConstant.UserRole.USER),
    MONITORING(SecurityConstant.UserRole.MONITORING),

    ANONYMOUS(SecurityConstant.UserRole.ANONYMOUS);

    private final String value;
    private static final Map<String, UserRole> CONSTANTS = createConstants();

    UserRole(final String value) {
        this.value = value;
    }

    @JsonCreator
    public static UserRole of(String role) {
        return Optional.ofNullable(CONSTANTS.get(role))
                .orElseThrow(() -> new IllegalArgumentException(String.format("Not Supported for this role: %s", role)));
    }

    public static Set<UserRole> parseRoles(String strRoles) {
        return Arrays.stream(StringUtils.tokenizeToStringArray(strRoles.replace("'", ""), ","))
                .map(UserRole::of)
                .collect(Collectors.toSet());
    }

    private static Map<String, UserRole> createConstants() {
        final var constants = new HashMap<String, UserRole>();
        Arrays.stream(values()).forEach(role -> {
            constants.put(role.name(), role);
            constants.put(role.value, role);
        });
        return Map.copyOf(constants);
    }

    @Override
    public String toString() {
        return value;
    }

    @JsonValue
    public String getClaimValue() {
        return name();
    }

    public String getValue() {
        return value;
    }
}
