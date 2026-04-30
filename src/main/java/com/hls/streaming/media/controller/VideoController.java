package com.hls.streaming.media.controller;

import com.hls.streaming.media.api.VideoApi;
import com.hls.streaming.infrastructure.config.error.ErrorCodeConfig;
import com.hls.streaming.common.dtos.PageResponse;
import com.hls.streaming.media.dto.*;
import com.hls.streaming.infrastructure.security.utils.SecurityUtils;
import com.hls.streaming.media.service.multipart.MultipartService;
import com.hls.streaming.media.service.query.VideoQueryService;
import com.hls.streaming.media.service.upload.VideoUploadService;
import com.hls.streaming.media.utils.MediaUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class VideoController implements VideoApi {

    private final VideoUploadService videoUploadService;
    private final MultipartService multipartService;
    private final VideoQueryService videoQueryService;
    private final ErrorCodeConfig errorCodeConfig;

    @Override
    public VideoResponse getVideoById(String id) {
        return videoQueryService.getVideoById(id);
    }

    @Override
    public PageResponse<VideoResponse> getMyVideos(Pageable pageable) {
        var userId = SecurityUtils.getUserIdFromToken(errorCodeConfig);
        return videoQueryService.getVideosByUser(userId, pageable);
    }

    @Override
    public PageResponse<VideoResponse> getVideosByUserId(String userId, Pageable pageable) {
        return videoQueryService.getVideosByUser(userId, pageable);
    }

    @Override
    public VideoUploadResponse uploadVideo(final MultipartFile file, final String title, final String description) {
        MediaUtils.validateVideoFile(file);

        var safeFileName = MediaUtils.generateSafeFileName(file.getOriginalFilename());
        var userId = SecurityUtils.getUserIdFromToken(errorCodeConfig);

        return videoUploadService.uploadRawVideo(userId, file, title, description, safeFileName);
    }

    @Override
    public MultipartInitResponse initMultipartUpload(final String fileName, final String contentType) {
        var userId = SecurityUtils.getUserIdFromToken(errorCodeConfig);
        return multipartService.initMultipartUpload(userId, fileName, contentType);
    }

    @Override
    public MultipartUploadUrlResponse getUploadPartUrl(final String key, final String uploadId, final int partNumber) {
        return multipartService.getUploadPartUrl(key, uploadId, partNumber);
    }

    @Override
    public VideoUploadResponse completeMultipartUpload(final CompleteMultipartRequest request) {
        var userId = SecurityUtils.getUserIdFromToken(errorCodeConfig);
        return multipartService.completeMultipartUpload(userId, request);
    }

    @Override
    public void abortMultipartUpload(final String key, final String uploadId) {
        multipartService.abortMultipartUpload(key, uploadId);
    }
}
