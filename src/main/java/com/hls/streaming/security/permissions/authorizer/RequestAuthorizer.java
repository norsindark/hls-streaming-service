package com.hls.streaming.security.permissions.authorizer;

import com.hls.streaming.security.models.TokenClaim;
import jakarta.servlet.http.HttpServletRequest;

public interface RequestAuthorizer {

    boolean hasSupported(String authorizeExpression);

    boolean isAccessible(String permissionsExpression, HttpServletRequest request, TokenClaim tokenClaim);
}
