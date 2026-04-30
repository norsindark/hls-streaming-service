package com.hls.streaming.infrastructure.security.context;

import com.hls.streaming.security.authentication.model.FullTokenClaim;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;

@Getter
@Setter
public abstract class AppSecurityContext {

    private String authorizationToken;
    private FullTokenClaim fullTokenClaim;
    private RequestSecurityContext context;

    protected AppSecurityContext(String authorizationToken, FullTokenClaim fullTokenClaim, RequestSecurityContext context) {
        this.authorizationToken = authorizationToken;
        this.fullTokenClaim = fullTokenClaim;
        this.context = context;
    }

    private Authentication authentication;
}
