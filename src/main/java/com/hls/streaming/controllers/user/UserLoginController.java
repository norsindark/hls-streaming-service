package com.hls.streaming.controllers.user;

import com.hls.streaming.api.user.UserLoginApi;
import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.dtos.user.IdentifyUserRequest;
import com.hls.streaming.dtos.user.RegisterUserRequest;
import com.hls.streaming.dtos.user.VerifyPasswordRequest;
import com.hls.streaming.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserLoginController implements UserLoginApi {

    private final UserService userService;

    @Override
    public UserAccessResponse register(final RegisterUserRequest request) {
        return null;
    }

    @Override
    public UserAccessResponse identifyUser(final IdentifyUserRequest request) {
        return userService.identifyUser(request);
    }

    @Override
    public UserAccessResponse verifyPassword(final VerifyPasswordRequest request) {
        return userService.verifyPassword(request);
    }
}
