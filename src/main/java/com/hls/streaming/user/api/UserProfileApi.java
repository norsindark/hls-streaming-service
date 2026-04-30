package com.hls.streaming.user.api;

import com.hls.streaming.user.dto.UserLiteResponse;
import com.hls.streaming.security.constants.SecurityConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "User Profile API", description = "APIs for managing user profiles")
public interface UserProfileApi {

    @Operation(summary = "Get User Profile")
    @GetMapping("/api/v1/users/profile")
    @PreAuthorize("hasRole('" + SecurityConstant.UserRole.USER + "')")
    @ResponseStatus(HttpStatus.OK)
    UserLiteResponse getProfile();
}
