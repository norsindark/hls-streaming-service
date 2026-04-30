package com.hls.streaming.infrastructure.security.utils;

import com.hls.streaming.security.authentication.model.UserRole;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class UserRoleUtils {

    public static Set<UserRole> parseRoles(final String roles) {
        return Arrays.stream(StringUtils.tokenizeToStringArray(roles.replace("'", ""), ","))
                .map(UserRole::valueOf)
                .collect(Collectors.toSet());
    }
}
