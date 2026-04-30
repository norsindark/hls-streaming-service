package com.hls.streaming.infrastructure.security.context;

import com.hls.streaming.security.authentication.model.FullTokenClaim;
import lombok.Builder;

public class HttpSecurityContext extends AppSecurityContext {

    @Builder(builderMethodName = "httpSecurityContextBuilder")
    public HttpSecurityContext(final String authorizationToken, final FullTokenClaim fullTokenClaim) {
        super(authorizationToken, fullTokenClaim, RequestSecurityContext.HTTP);
    }
}
