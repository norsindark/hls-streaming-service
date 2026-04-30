package com.hls.streaming.user.api;

import com.hls.streaming.security.authentication.dto.UserAccessResponse;
import com.hls.streaming.user.dto.IdentifyUserRequest;
import com.hls.streaming.user.dto.RegisterUserRequest;
import com.hls.streaming.user.dto.VerifyPasswordRequest;
import com.hls.streaming.security.constants.SecurityConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "User API", description = "User authentication and management APIs")
public interface UserLoginApi {

    @Operation(summary = "Register a new user")
    @PostMapping("/api/v1/public/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserAccessResponse register(@Valid @RequestBody RegisterUserRequest request);

    @Operation(summary = "Identify user")
    @PostMapping("/api/v1/public/users/identify")
    @ResponseStatus(HttpStatus.OK)
    UserAccessResponse identifyUser(@Valid @RequestBody IdentifyUserRequest request);

    @Operation(summary = "Verify password")
    @PreAuthorize("hasRole('" + SecurityConstant.UserRole.USER + "')")
    @PostMapping("/api/v1/users/verify-password")
    @ResponseStatus(HttpStatus.OK)
    UserAccessResponse verifyPassword(@Valid @RequestBody VerifyPasswordRequest request);
}
