package com.hls.streaming.user.controller;

import com.hls.streaming.user.api.UserLoginApi;
import com.hls.streaming.infrastructure.config.error.ErrorCodeConfig;
import com.hls.streaming.security.authentication.dto.UserAccessResponse;
import com.hls.streaming.user.dto.IdentifyUserRequest;
import com.hls.streaming.user.dto.RegisterUserRequest;
import com.hls.streaming.user.dto.VerifyPasswordRequest;
import com.hls.streaming.common.exception.BadRequestException;
import com.hls.streaming.infrastructure.security.utils.SecurityUtils;
import com.hls.streaming.user.service.command.UserAuthService;
import com.hls.streaming.user.service.command.UserRegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import static com.hls.streaming.common.constant.ErrorConfigConstants.TOKEN_IS_MISSING;

@RestController
@RequiredArgsConstructor
public class UserLoginController implements UserLoginApi {

    private final UserRegisterService registerService;
    private final UserAuthService authService;
    private final ErrorCodeConfig errorCodeConfig;

    @Override
    public UserAccessResponse register(final RegisterUserRequest request) {
        return registerService.register(request);
    }

    @Override
    public UserAccessResponse identifyUser(final IdentifyUserRequest request) {
        return authService.identify(request);
    }

    @Override
    public UserAccessResponse verifyPassword(final VerifyPasswordRequest request) {

        var tokenClaim = SecurityUtils.getTokenClaimFromSecurityContext()
                .orElseThrow(() -> new BadRequestException(
                        errorCodeConfig.getMessage(TOKEN_IS_MISSING)));

        request.setUserId(tokenClaim.getUserId());
        return authService.verifyPassword(request);
    }
}
