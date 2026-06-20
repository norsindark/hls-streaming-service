package com.hls.streaming.media.service.multipart;

import com.hls.streaming.common.constant.ErrorConfigConstants;
import com.hls.streaming.common.exception.NotFoundException;
import com.hls.streaming.infrastructure.config.error.ErrorCodeConfig;
import com.hls.streaming.infrastructure.config.properties.StorageConfig;
import com.hls.streaming.infrastructure.metrics.MetricsConstants;
import com.hls.streaming.infrastructure.storage.S3Client;
import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.media.domain.enums.VideoStatus;
import com.hls.streaming.media.domain.repository.VideoRepository;
import com.hls.streaming.media.dto.*;
import com.hls.streaming.media.event.OnUploadVideoEvent;
import com.hls.streaming.media.service.processing.ffmpeg.FfmpegProcessRegistry;
import com.hls.streaming.media.utils.MediaUtils;
import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MultipartService {

    private final S3Client s3Client;
    private final StorageConfig storageConfig;
    private final VideoRepository videoRepository;
    private final FfmpegProcessRegistry processRegistry;
    private final ApplicationEventPublisher publisher;
    private final ErrorCodeConfig errorCodeConfig;
    private final MeterRegistry registry;

    private final Counter multipartInitCounter;
    private final Counter multipartCompleteCounter;
    private final Counter multipartAbortCounter;
    private final Timer multipartCompleteTimer;

    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024 * 1024;

    public MultipartService(
            final S3Client s3Client,
            final StorageConfig storageConfig,
            final VideoRepository videoRepository,
            final FfmpegProcessRegistry processRegistry,
            final ApplicationEventPublisher publisher,
            final ErrorCodeConfig errorCodeConfig,
            final MeterRegistry registry) {

        this.s3Client = s3Client;
        this.storageConfig = storageConfig;
        this.videoRepository = videoRepository;
        this.processRegistry = processRegistry;
        this.publisher = publisher;
        this.errorCodeConfig = errorCodeConfig;
        this.registry = registry;

        this.multipartInitCounter = Counter.builder(MetricsConstants.Multipart.INIT)
                .description("Multipart upload init")
                .register(registry);

        this.multipartCompleteCounter = Counter.builder(MetricsConstants.Multipart.COMPLETE)
                .description("Multipart upload complete")
                .register(registry);

        this.multipartAbortCounter = Counter.builder(MetricsConstants.Multipart.ABORT)
                .description("Multipart upload abort")
                .register(registry);

        this.multipartCompleteTimer = Timer.builder(MetricsConstants.Multipart.COMPLETE_LATENCY)
                .publishPercentileHistogram()
                .register(registry);
    }

    public MultipartInitResponse initMultipartUpload(final String userId, final String fileName, final String contentType) {

        multipartInitCounter.increment();

        final String key = MediaUtils.generateKey(storageConfig.getRawVideoPrefix(), userId, fileName);
        final String uploadId = s3Client.createMultipartUpload(key, contentType);

        return MultipartInitResponse.builder()
                .key(key)
                .uploadId(uploadId)
                .build();
    }

    public MultipartUploadUrlResponse getUploadPartUrl(final String key, final String uploadId, final int partNumber) {

        final String url = s3Client.generatePresignedUploadPartUrl(key, uploadId, partNumber);

        return MultipartUploadUrlResponse.builder()
                .url(url)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public VideoUploadResponse completeMultipartUpload(final String userId, final CompleteMultipartRequest request) {

        final Timer.Sample sample = Timer.start(registry);

        try {
            validate(userId, request);

            final List<CompletedPart> parts = request.getParts().stream()
                    .map(p -> CompletedPart.builder()
                            .partNumber(p.getPartNumber())
                            .eTag(p.getEtag())
                            .build())
                    .toList();

            s3Client.completeMultipartUpload(
                    request.getKey(),
                    request.getUploadId(),
                    parts);

            final String key = request.getKey();
            final String folder = key.substring(0, key.lastIndexOf("/"));
            final String fileName = Path.of(key).getFileName().toString();

            final Video saved = videoRepository.save(Video.builder()
                    .userId(userId)
                    .title(StringUtils.defaultString(request.getTitle()))
                    .description(StringUtils.defaultString(request.getDescription()))
                    .objectKey(key)
                    .folder(folder)
                    .fileName(fileName)
                    .contentType(request.getContentType())
                    .fileSize(request.getSize())
                    .status(VideoStatus.CREATED)
                    .build());

            publisher.publishEvent(OnUploadVideoEvent.builder()
                    .videoId(saved.getId())
                    .build());

            multipartCompleteCounter.increment();

            return VideoUploadResponse.builder()
                    .videoId(saved.getId())
                    .status(saved.getStatus())
                    .build();

        } finally {
            sample.stop(multipartCompleteTimer);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void abortMultipartUpload(final AbortUploadVideoRequest request) {

        multipartAbortCounter.increment();

        final Video video = videoRepository
                .findVideoByObjectKeyAndUserId(request.getKey(), request.getUserId())
                .orElseThrow(() -> new NotFoundException(
                        errorCodeConfig.getMessage(ErrorConfigConstants.VIDEO_NOT_FOUND)));

        s3Client.abortMultipartUpload(video.getObjectKey(), request.getUploadId());
        processRegistry.cancel(video.getObjectKey());
    }

    private void validate(final String userId, final CompleteMultipartRequest request) {

        if (request.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File too large");
        }

        if (!request.getKey().contains(userId)) {
            throw new SecurityException("Invalid key");
        }
    }
}
