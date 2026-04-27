package com.hls.streaming.api.media;

import com.hls.streaming.dtos.PageResponse;
import com.hls.streaming.dtos.media.*;
import com.hls.streaming.security.constants.SecurityConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Video Api", description = "Video api")
public interface VideoApi {

    @Operation(summary = "Get video by id")
    @GetMapping("/api/v1/public/videos/{id}")
    VideoResponse getVideoById(@PathVariable String id);

    @Operation(summary = "Get my videos (from token)")
    @PreAuthorize("hasRole('" + SecurityConstant.UserRole.USER + "')")
    @GetMapping("/api/v1/videos/me")
    PageResponse<VideoResponse> getMyVideos(Pageable pageable);

    @Operation(summary = "Get videos by userId")
    @GetMapping("/api/v1/public/videos/user/{userId}")
    PageResponse<VideoResponse> getVideosByUserId(@PathVariable String userId, Pageable pageable);

    @Operation(summary = "Upload raw video and trigger processing")
    @PreAuthorize("hasRole('" + SecurityConstant.UserRole.USER + "')")
    @PostMapping("/api/v1/videos/upload")
    VideoUploadResponse uploadVideo(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title,
            @RequestPart(value = "description", required = false) String description);

    @Operation(summary = "Init multipart upload")
    @PostMapping("/api/v1/videos/multipart/init")
    MultipartInitResponse initMultipartUpload(
            @RequestParam String fileName,
            @RequestParam String contentType);

    @Operation(summary = "Get presigned url for upload part")
    @GetMapping("/api/v1/videos/multipart/url")
    MultipartUploadUrlResponse getUploadPartUrl(
            @RequestParam String key,
            @RequestParam String uploadId,
            @RequestParam int partNumber);

    @Operation(summary = "Complete multipart upload")
    @PostMapping("/api/v1/videos/multipart/complete")
    VideoUploadResponse completeMultipartUpload(
            @RequestBody CompleteMultipartRequest request);
}
