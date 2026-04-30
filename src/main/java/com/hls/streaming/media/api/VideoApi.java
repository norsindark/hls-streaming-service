package com.hls.streaming.media.api;

import com.hls.streaming.common.dtos.PageResponse;
import com.hls.streaming.media.dto.*;
import com.hls.streaming.security.constants.SecurityConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Video Api", description = "Video api")
public interface VideoApi {

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

    @Operation(summary = "Upload raw video and trigger processing")
    @PreAuthorize("hasRole('" + SecurityConstant.UserRole.USER + "')")
    @PostMapping("/api/v1/videos/upload")
    @ResponseStatus(HttpStatus.OK)
    VideoUploadResponse uploadVideo(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title,
            @RequestPart(value = "description", required = false) String description);

    @Operation(summary = "Init multipart upload")
    @PreAuthorize("hasRole('" + SecurityConstant.UserRole.USER + "')")
    @PostMapping("/api/v1/videos/multipart/init")
    @ResponseStatus(HttpStatus.OK)
    MultipartInitResponse initMultipartUpload(
            @RequestParam String fileName,
            @RequestParam String contentType);

    @Operation(summary = "Get presigned url for upload part")
    @PreAuthorize("hasRole('" + SecurityConstant.UserRole.USER + "')")
    @GetMapping("/api/v1/videos/multipart/url")
    @ResponseStatus(HttpStatus.OK)
    MultipartUploadUrlResponse getUploadPartUrl(
            @RequestParam String key,
            @RequestParam String uploadId,
            @RequestParam int partNumber);

    @Operation(summary = "Complete multipart upload")
    @PreAuthorize("hasRole('" + SecurityConstant.UserRole.USER + "')")
    @PostMapping("/api/v1/videos/multipart/complete")
    @ResponseStatus(HttpStatus.OK)
    VideoUploadResponse completeMultipartUpload(
            @RequestBody CompleteMultipartRequest request);

    @Operation(summary = "Abort multipart upload")
    @PreAuthorize("hasRole('" + SecurityConstant.UserRole.USER + "')")
    @DeleteMapping("/api/v1/videos/multipart/abort")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void abortMultipartUpload(
            @RequestParam String key,
            @RequestParam String uploadId);
}
