package com.hls.streaming.services.media.multipart;

import com.hls.streaming.config.properties.StorageConfig;
import com.hls.streaming.documents.media.Video;
import com.hls.streaming.dtos.events.OnUploadVideoEvent;
import com.hls.streaming.dtos.media.*;
import com.hls.streaming.enums.UploadProcess;
import com.hls.streaming.enums.VideoStatus;
import com.hls.streaming.repositories.media.VideoRepository;
import com.hls.streaming.storage.S3Client;
import com.hls.streaming.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class MultipartService {

    private final S3Client s3Client;
    private final StorageConfig storageConfig;
    private final VideoRepository videoRepository;
    private final ApplicationEventPublisher publisher;

    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024 * 1024;

    public MultipartInitResponse initMultipartUpload(final String userId, final String fileName, final String contentType) {

        var safeFileName = FileUtils.generateSafeFileName(fileName);
        var key = storageConfig.getRawVideoPrefix() + userId + "/" + safeFileName;
        var uploadId = s3Client.createMultipartUpload(key, contentType);

        return MultipartInitResponse.builder()
                .key(key)
                .uploadId(uploadId)
                .build();
    }

    public MultipartUploadUrlResponse getUploadPartUrl(final String key, final String uploadId, final int partNumber) {

        var url = s3Client.generatePresignedUploadPartUrl(key, uploadId, partNumber);
        return MultipartUploadUrlResponse.builder()
                .url(url)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public VideoUploadResponse completeMultipartUpload(final String userId, final CompleteMultipartRequest request) {

        validate(userId, request);

        var parts = request.getParts().stream()
                .map(p -> CompletedPart.builder()
                        .partNumber(p.getPartNumber())
                        .eTag(p.getEtag())
                        .build())
                .toList();

        s3Client.completeMultipartUpload(
                request.getKey(),
                request.getUploadId(),
                parts);

        var key = request.getKey();
        var folder = key.substring(0, key.lastIndexOf("/"));
        var fileName = Path.of(key).getFileName().toString();

        var saved = videoRepository.save(Video.builder()
                .userId(userId)
                .title(StringUtils.defaultString(request.getTitle()))
                .description(StringUtils.defaultString(request.getDescription()))
                .folder(folder)
                .fileName(fileName)
                .contentType(request.getContentType())
                .fileSize(request.getSize())
                .status(VideoStatus.CREATED)
                .build());

        publisher.publishEvent(OnUploadVideoEvent.builder()
                .videoId(saved.getId())
                .build());

        return VideoUploadResponse.builder()
                .videoId(saved.getId())
                .status(saved.getStatus())
                .build();
    }

    public void abortMultipartUpload(String key, String uploadId) {
        s3Client.abortMultipartUpload(key, uploadId);
    }

    private void validate(String userId, CompleteMultipartRequest request) {

        if (request.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File too large");
        }

        if (!request.getKey().contains(userId)) {
            throw new SecurityException("Invalid key");
        }
    }
}
