package com.hls.streaming.controllers.user;

import com.hls.streaming.api.user.UserProfileApi;
import com.hls.streaming.config.error.ErrorCodeConfig;
import com.hls.streaming.dtos.user.UserLiteResponse;
import com.hls.streaming.exception.BadRequestException;
import com.hls.streaming.security.utils.SecurityUtils;
import com.hls.streaming.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import static com.hls.streaming.constant.ErrorConfigConstants.TOKEN_IS_MISSING;

@RestController
@RequiredArgsConstructor
public class UserProfileController implements UserProfileApi {

    private final ErrorCodeConfig errorCodeConfig;
    private final UserService userService;

    @Override
    public UserLiteResponse getProfile() {
        final var tokenClaim = SecurityUtils.getTokenClaimFromSecurityContext()
                .orElseThrow(() -> new BadRequestException(errorCodeConfig.getMessage(TOKEN_IS_MISSING)));
        return userService.getProfile(tokenClaim.getUserId());
    }
}
