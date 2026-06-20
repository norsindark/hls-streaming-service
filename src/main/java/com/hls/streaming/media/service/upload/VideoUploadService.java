package com.hls.streaming.media.service.upload;

import com.hls.streaming.infrastructure.config.properties.StorageConfig;
import com.hls.streaming.infrastructure.metrics.MetricsConstants;
import com.hls.streaming.infrastructure.storage.S3Client;
import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.media.domain.enums.VideoStatus;
import com.hls.streaming.media.domain.repository.VideoRepository;
import com.hls.streaming.media.dto.VideoUploadResponse;
import com.hls.streaming.media.event.OnUploadVideoEvent;
import com.hls.streaming.media.utils.MediaUtils;
import io.micrometer.core.instrument.*;
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
    private final MeterRegistry registry;

    private final Counter uploadSuccessCounter;
    private final Counter uploadFailureCounter;
    private final Timer uploadTimer;

    public VideoUploadService(
            final VideoRepository videoRepository,
            final S3Client s3Client,
            final StorageConfig storageConfig,
            final ApplicationEventPublisher publisher,
            final MeterRegistry registry) {

        this.videoRepository = videoRepository;
        this.s3Client = s3Client;
        this.storageConfig = storageConfig;
        this.publisher = publisher;
        this.registry = registry;

        this.uploadSuccessCounter = Counter.builder(MetricsConstants.Upload.SUCCESS)
                .register(registry);

        this.uploadFailureCounter = Counter.builder(MetricsConstants.Upload.FAILURE)
                .register(registry);

        this.uploadTimer = Timer.builder(MetricsConstants.Upload.LATENCY)
                .publishPercentileHistogram()
                .register(registry);
    }

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

        final Timer.Sample sample = Timer.start(registry);

        final String folder = storageConfig.getRawVideoPrefix() + userId;
        final String key = MediaUtils.generateKey(storageConfig.getRawVideoPrefix(), userId, fileName);
        final String contentType = StringUtils.defaultIfBlank(file.getContentType(), "video/mp4");

        final Video video = Video.builder()
                .userId(userId)
                .title(StringUtils.defaultString(title))
                .description(StringUtils.defaultString(description))
                .objectKey(key)
                .folder(folder)
                .fileName(fileName)
                .contentType(contentType)
                .fileSize(file.getSize())
                .status(VideoStatus.CREATED)
                .build();

        try {
            s3Client.uploadFile(folder, fileName, file.getInputStream(), contentType, file.getSize());

            uploadSuccessCounter.increment();

        } catch (Exception e) {
            uploadFailureCounter.increment();
            video.setStatus(VideoStatus.FAILED);
            videoRepository.save(video);
            throw new RuntimeException("Upload failed", e);
        } finally {
            sample.stop(uploadTimer);
        }

        final Video saved = videoRepository.save(video);

        publisher.publishEvent(OnUploadVideoEvent.builder()
                .videoId(saved.getId())
                .build());

        return VideoUploadResponse.builder()
                .videoId(saved.getId())
                .status(saved.getStatus())
                .build();
    }
}
