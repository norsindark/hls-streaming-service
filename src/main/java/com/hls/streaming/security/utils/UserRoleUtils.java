package com.hls.streaming.security.utils;

import com.hls.streaming.security.models.UserRole;
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
