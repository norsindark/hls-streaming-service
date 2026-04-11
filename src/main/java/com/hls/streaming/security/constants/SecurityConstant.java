package com.hls.streaming.security.constants;

import org.springframework.http.HttpHeaders;

public class SecurityConstant {

    public static final String AUTHORIZATION = HttpHeaders.AUTHORIZATION;
    public static final String BEARER = "Bearer";

    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";

    private SecurityConstant() {}

    public static class UserRole {
        public static final String ADMIN = "ROLE_ADMIN";
        public static final String SYSTEM = "ROLE_SYSTEM";
        public static final String USER = "ROLE_USER";

        public static final String ANONYMOUS = "ROLE_ANONYMOUS";

        private UserRole() {}
    }
}
