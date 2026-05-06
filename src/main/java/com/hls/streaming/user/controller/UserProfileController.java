package com.hls.streaming.user.controller;

import com.hls.streaming.user.api.UserProfileApi;
import com.hls.streaming.user.dto.UserLiteResponse;
import com.hls.streaming.security.context.CurrentUserProvider;
import com.hls.streaming.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserProfileController implements UserProfileApi {

    private final CurrentUserProvider currentUserProvider;
    private final UserQueryService queryService;

    @Override
    public UserLiteResponse getProfile() {
        final String userId = currentUserProvider.getUserId();
        return queryService.getProfile(userId);
    }
}
