package com.hls.streaming.security.context;

import com.hls.streaming.security.models.FullTokenClaim;
import lombok.Builder;

public class HttpSecurityContext extends AppSecurityContext {

    @Builder(builderMethodName = "httpSecurityContextBuilder")
    public HttpSecurityContext(final String authorizationToken, final FullTokenClaim fullTokenClaim) {
        super(authorizationToken, fullTokenClaim, RequestSecurityContext.HTTP);
    }
}
