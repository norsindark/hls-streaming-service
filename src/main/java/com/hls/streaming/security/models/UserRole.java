package com.hls.streaming.security.models;

import com.fasterxml.jackson.annotation.JsonValue;
import com.hls.streaming.security.constants.SecurityConstant;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum UserRole {

    ADMIN(SecurityConstant.UserRole.ADMIN), SYSTEM(SecurityConstant.UserRole.SYSTEM), USER(SecurityConstant.UserRole.USER),

    ANONYMOUS(SecurityConstant.UserRole.ANONYMOUS);

    private final String value;
    private static final Map<String, UserRole> CONSTANTS = Map.of(
            ADMIN.value, ADMIN,
            SYSTEM.value, SYSTEM,
            USER.value, USER,
            ANONYMOUS.value, ANONYMOUS);

    UserRole(final String value) {
        this.value = value;
    }

    public static UserRole of(String role) {
        return Optional.ofNullable(CONSTANTS.get(role))
                .orElseThrow(() -> new IllegalArgumentException(String.format("Not Supported for this role: %s", role)));
    }

    public static Set<UserRole> parseRoles(String strRoles) {
        return Arrays.stream(StringUtils.tokenizeToStringArray(strRoles.replace("'", ""), ","))
                .map(CONSTANTS::get)
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
