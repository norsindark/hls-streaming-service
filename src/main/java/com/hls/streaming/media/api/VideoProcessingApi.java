package com.hls.streaming.media.api;

import com.hls.streaming.media.dto.*;
import com.hls.streaming.security.constants.SecurityConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Video Api", description = "Video api")
public interface VideoProcessingApi {

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
    @PostMapping("/api/v1/videos/multipart/abort")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void abortMultipartUpload( @Valid @RequestBody AbortUploadVideoRequest request);
}
