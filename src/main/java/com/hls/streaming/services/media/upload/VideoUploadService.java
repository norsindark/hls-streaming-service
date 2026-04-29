package com.hls.streaming.services.media.upload;

import com.hls.streaming.config.properties.StorageConfig;
import com.hls.streaming.documents.media.Video;
import com.hls.streaming.dtos.events.OnUploadVideoEvent;
import com.hls.streaming.dtos.media.VideoUploadResponse;
import com.hls.streaming.enums.UploadProcess;
import com.hls.streaming.enums.VideoStatus;
import com.hls.streaming.repositories.media.VideoRepository;
import com.hls.streaming.storage.S3Client;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VideoUploadService {

    private final VideoRepository videoRepository;
    private final S3Client s3Client;
    private final StorageConfig storageConfig;
    private final ApplicationEventPublisher publisher;

    @Transactional(rollbackFor = Exception.class)
    public VideoUploadResponse uploadRawVideo(
            final String userId,
            final MultipartFile file,
            final String title,
            final String description,
            final String fileName) {

        if (Objects.isNull(file) || file.isEmpty() || StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("Invalid file");
        }

        var folder = storageConfig.getRawVideoPrefix() + userId;
        var contentType = StringUtils.defaultIfBlank(file.getContentType(), "video/mp4");

        var video = Video.builder()
                .userId(userId)
                .title(StringUtils.defaultString(title))
                .description(StringUtils.defaultString(description))
                .folder(folder)
                .fileName(fileName)
                .contentType(contentType)
                .fileSize(file.getSize())
                .status(VideoStatus.CREATED)
                .build();

        try {
            s3Client.uploadFile(folder, fileName, file.getInputStream(), contentType, file.getSize());
        } catch (Exception e) {
            video.setStatus(VideoStatus.FAILED);
            videoRepository.save(video);
            throw new RuntimeException("Upload failed", e);
        }

        var saved = videoRepository.save(video);

        publisher.publishEvent(OnUploadVideoEvent.builder()
                .videoId(saved.getId())
                .build());

        return VideoUploadResponse.builder()
                .videoId(saved.getId())
                .status(saved.getStatus())
                .build();
    }
}
