package com.hls.streaming.controllers.media;

import com.hls.streaming.api.media.VideoApi;
import com.hls.streaming.config.error.ErrorCodeConfig;
import com.hls.streaming.dtos.PageResponse;
import com.hls.streaming.dtos.media.*;
import com.hls.streaming.security.utils.SecurityUtils;
import com.hls.streaming.services.media.VideoService;
import com.hls.streaming.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class VideoController implements VideoApi {

    private final VideoService videoService;
    private final ErrorCodeConfig errorCodeConfig;

    @Override
    public VideoResponse getVideoById(String id) {
        return videoService.getVideoById(id);
    }

    @Override
    public PageResponse<VideoResponse> getMyVideos(Pageable pageable) {
        var userId = SecurityUtils.getUserIdFromToken(errorCodeConfig);
        return videoService.getVideosByUser(userId, pageable);
    }

    @Override
    public PageResponse<VideoResponse> getVideosByUserId(String userId, Pageable pageable) {
        return videoService.getVideosByUser(userId, pageable);
    }

    @Override
    public VideoUploadResponse uploadVideo(final MultipartFile file, final String title, final String description) {
        FileUtils.validateVideoFile(file);

        var safeFileName = FileUtils.generateSafeFileName(file.getOriginalFilename());
        var userId = SecurityUtils.getUserIdFromToken(errorCodeConfig);

        return videoService.processUploadRawVideo(userId, file, title, description, safeFileName);
    }

    @Override
    public MultipartInitResponse initMultipartUpload(final String fileName, final String contentType) {
        var userId = SecurityUtils.getUserIdFromToken(errorCodeConfig);
        return videoService.initMultipartUpload(userId, fileName, contentType);
    }

    @Override
    public MultipartUploadUrlResponse getUploadPartUrl(final String key, final String uploadId, final int partNumber) {
        return videoService.getUploadPartUrl(key, uploadId, partNumber);
    }

    @Override
    public VideoUploadResponse completeMultipartUpload(final CompleteMultipartRequest request) {
        var userId = SecurityUtils.getUserIdFromToken(errorCodeConfig);
        return videoService.completeMultipartUpload(userId, request);
    }
}
