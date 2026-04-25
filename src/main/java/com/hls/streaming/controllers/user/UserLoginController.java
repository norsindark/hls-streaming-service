package com.hls.streaming.controllers.user;

import com.hls.streaming.api.user.UserLoginApi;
import com.hls.streaming.config.error.ErrorCodeConfig;
import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.dtos.user.IdentifyUserRequest;
import com.hls.streaming.dtos.user.RegisterUserRequest;
import com.hls.streaming.dtos.user.VerifyPasswordRequest;
import com.hls.streaming.exception.BadRequestException;
import com.hls.streaming.security.utils.SecurityUtils;
import com.hls.streaming.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import static com.hls.streaming.constant.ErrorConfigConstants.TOKEN_IS_MISSING;

@RestController
@RequiredArgsConstructor
public class UserLoginController implements UserLoginApi {

    private final UserService userService;
    private final ErrorCodeConfig errorCodeConfig;

    @Override
    public UserAccessResponse register(final RegisterUserRequest request) {
        return userService.register(request);
    }

    @Override
    public UserAccessResponse identifyUser(final IdentifyUserRequest request) {
        return userService.identifyUser(request);
    }

    @Override
    public UserAccessResponse verifyPassword(final VerifyPasswordRequest request) {
        final var tokenClaim = SecurityUtils.getTokenClaimFromSecurityContext()
                .orElseThrow(() -> new BadRequestException(errorCodeConfig.getMessage(TOKEN_IS_MISSING)));
        request.setUserId(tokenClaim.getUserId());
        return userService.verifyPassword(request);
    }
}
