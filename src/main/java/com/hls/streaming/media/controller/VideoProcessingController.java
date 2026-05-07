package com.hls.streaming.media.controller;

import com.hls.streaming.media.api.VideoProcessingApi;
import com.hls.streaming.media.dto.*;
import com.hls.streaming.media.service.multipart.MultipartService;
import com.hls.streaming.media.service.upload.VideoUploadService;
import com.hls.streaming.media.utils.MediaUtils;
import com.hls.streaming.security.context.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class VideoProcessingController implements VideoProcessingApi {

    private final VideoUploadService videoUploadService;
    private final MultipartService multipartService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public VideoUploadResponse uploadVideo(final MultipartFile file, final String title, final String description) {
        MediaUtils.validateVideoFile(file);

        var safeFileName = MediaUtils.generateSafeFileName(file.getOriginalFilename());
        var userId = currentUserProvider.getUserId();

        return videoUploadService.uploadRawVideo(userId, file, title, description, safeFileName);
    }

    @Override
    public MultipartInitResponse initMultipartUpload(final String fileName, final String contentType) {
        var userId = currentUserProvider.getUserId();
        return multipartService.initMultipartUpload(userId, fileName, contentType);
    }

    @Override
    public MultipartUploadUrlResponse getUploadPartUrl(final String key, final String uploadId, final int partNumber) {
        return multipartService.getUploadPartUrl(key, uploadId, partNumber);
    }

    @Override
    public VideoUploadResponse completeMultipartUpload(final CompleteMultipartRequest request) {
        var userId = currentUserProvider.getUserId();
        return multipartService.completeMultipartUpload(userId, request);
    }

    @Override
    public void abortMultipartUpload(final AbortUploadVideoRequest request) {
        var userId = currentUserProvider.getUserId();
        request.setUserId(userId);
        multipartService.abortMultipartUpload(request);
    }
}
