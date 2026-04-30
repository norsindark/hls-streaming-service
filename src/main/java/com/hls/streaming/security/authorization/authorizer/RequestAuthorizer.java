package com.hls.streaming.security.authorization.authorizer;

import com.hls.streaming.security.authentication.model.TokenClaim;
import jakarta.servlet.http.HttpServletRequest;

public interface RequestAuthorizer {

    boolean hasSupported(String authorizeExpression);

    boolean isAccessible(String permissionsExpression, HttpServletRequest request, TokenClaim tokenClaim);
}
