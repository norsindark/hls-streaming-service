package com.hls.streaming.api.user;

import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.dtos.user.IdentifyUserRequest;
import com.hls.streaming.dtos.user.RegisterUserRequest;
import com.hls.streaming.dtos.user.VerifyPasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "User API", description = "User authentication and management APIs")
public interface UserLoginApi {

    @Operation(summary = "Register a new user",
            description = "Create a new user account and return official Access and Refresh tokens")
    @PostMapping("/api/v1/public/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserAccessResponse register(@Valid @RequestBody RegisterUserRequest request);

    @Operation(summary = "Step 1: Identify user",
            description = "Check if email or username exists and return a temporary verification token")
    @PostMapping("/api/v1/public/users/identify")
    @ResponseStatus(HttpStatus.OK)
    UserAccessResponse identifyUser(@Valid @RequestBody IdentifyUserRequest request);

    @Operation(summary = "Step 2: Verify password", description = "Verify password and return official Access and Refresh tokens")
    @PostMapping("/api/v1/users/verify-password")
    @ResponseStatus(HttpStatus.OK)
    UserAccessResponse verifyPassword(@Valid @RequestBody VerifyPasswordRequest request);
}
