package com.hls.streaming.user.controller;

import com.hls.streaming.user.api.UserProfileApi;
import com.hls.streaming.infrastructure.config.error.ErrorCodeConfig;
import com.hls.streaming.user.dto.UserLiteResponse;
import com.hls.streaming.common.exception.BadRequestException;
import com.hls.streaming.infrastructure.security.utils.SecurityUtils;
import com.hls.streaming.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import static com.hls.streaming.common.constant.ErrorConfigConstants.TOKEN_IS_MISSING;

@RestController
@RequiredArgsConstructor
public class UserProfileController implements UserProfileApi {

    private final ErrorCodeConfig errorCodeConfig;
    private final UserQueryService queryService;

    @Override
    public UserLiteResponse getProfile() {

        var tokenClaim = SecurityUtils.getTokenClaimFromSecurityContext()
                .orElseThrow(() -> new BadRequestException(
                        errorCodeConfig.getMessage(TOKEN_IS_MISSING)));

        return queryService.getProfile(tokenClaim.getUserId());
    }
}
