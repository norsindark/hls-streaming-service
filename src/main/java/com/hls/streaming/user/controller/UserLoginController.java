package com.hls.streaming.user.controller;

import com.hls.streaming.security.authentication.dto.UserAccessResponse;
import com.hls.streaming.security.context.CurrentUserProvider;
import com.hls.streaming.user.api.UserLoginApi;
import com.hls.streaming.user.dto.IdentifyUserRequest;
import com.hls.streaming.user.dto.RegisterUserRequest;
import com.hls.streaming.user.dto.VerifyPasswordRequest;
import com.hls.streaming.user.service.command.UserAuthService;
import com.hls.streaming.user.service.command.UserRegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserLoginController implements UserLoginApi {

    private final UserRegisterService registerService;
    private final UserAuthService authService;
    private final CurrentUserProvider currentUserProvider;

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

        final String userId = currentUserProvider.getUserId();

        request.setUserId(userId);
        return authService.verifyPassword(request);
    }
}
