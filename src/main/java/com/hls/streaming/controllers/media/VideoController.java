package com.hls.streaming.controllers.media;

import com.hls.streaming.api.media.VideoApi;
import com.hls.streaming.config.error.ErrorCodeConfig;
import com.hls.streaming.dtos.media.VideoUploadResponse;
import com.hls.streaming.security.utils.SecurityUtils;
import com.hls.streaming.services.media.VideoService;
import com.hls.streaming.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class VideoController implements VideoApi {

    private final VideoService videoService;
    private final ErrorCodeConfig errorCodeConfig;

    @Override
    public VideoUploadResponse uploadVideo(final MultipartFile file, final String title, final String description) {
        FileUtils.validateVideoFile(file);

        var safeFileName = FileUtils.generateSafeFileName(file.getOriginalFilename());
        var userId = SecurityUtils.getUserIdFromToken(errorCodeConfig);

        return videoService.processUploadRawVideo(userId, file, title, description, safeFileName);
    }
}
