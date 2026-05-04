package com.hls.streaming.security.context;

import com.hls.streaming.infrastructure.config.error.ErrorCodeConfig;
import com.hls.streaming.infrastructure.security.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProviderImpl implements CurrentUserProvider {

    private final ErrorCodeConfig errorCodeConfig;

    @Override
    public String getUserId() {
        return SecurityUtils.getUserIdFromToken(errorCodeConfig);
    }
}
