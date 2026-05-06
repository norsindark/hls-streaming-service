package com.hls.streaming.media.controller;

import com.hls.streaming.common.dtos.PageResponse;
import com.hls.streaming.media.api.VideoPublicApi;
import com.hls.streaming.media.dto.VideoResponse;
import com.hls.streaming.media.service.query.VideoQueryService;
import com.hls.streaming.security.context.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VideoPublicController implements VideoPublicApi {

    private final VideoQueryService videoQueryService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public VideoResponse getVideoById(String id) {
        return videoQueryService.getVideoById(id);
    }

    @Override
    public PageResponse<VideoResponse> getMyVideos(Pageable pageable) {
        var userId = currentUserProvider.getUserId();
        return videoQueryService.getVideosByUser(userId, pageable);
    }

    @Override
    public PageResponse<VideoResponse> getVideosByUserId(String userId, Pageable pageable) {
        return videoQueryService.getVideosByUser(userId, pageable);
    }

    @Override
    public PageResponse<VideoResponse> getHomeFeed(final String cursor, final Pageable pageable) {
        return videoQueryService.getHomeFeed(cursor, pageable);
    }
}
