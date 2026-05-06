package com.hls.streaming.media.api;

import com.hls.streaming.common.dtos.PageResponse;
import com.hls.streaming.media.dto.VideoResponse;
import com.hls.streaming.security.constants.SecurityConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Video Query Api", description = "Video query api")
public interface VideoPublicApi {

    @Operation(summary = "Get video by id")
    @GetMapping("/api/v1/public/videos/{id}")
    @ResponseStatus(HttpStatus.OK)
    VideoResponse getVideoById(@PathVariable String id);

    @Operation(summary = "Get my videos (from token)")
    @PreAuthorize("hasRole('" + SecurityConstant.UserRole.USER + "')")
    @GetMapping("/api/v1/videos/me")
    @ResponseStatus(HttpStatus.OK)
    PageResponse<VideoResponse> getMyVideos(Pageable pageable);

    @Operation(summary = "Get videos by userId")
    @GetMapping("/api/v1/public/videos/user/{userId}")
    @ResponseStatus(HttpStatus.OK)
    PageResponse<VideoResponse> getVideosByUserId(@PathVariable String userId, Pageable pageable);

    @Operation(summary = "Get home feed (YouTube style)")
    @GetMapping("/api/v1/public/videos/feed")
    @ResponseStatus(HttpStatus.OK)
    PageResponse<VideoResponse> getHomeFeed(
            @RequestParam(required = false) final String cursor,
            final Pageable pageable);
}
