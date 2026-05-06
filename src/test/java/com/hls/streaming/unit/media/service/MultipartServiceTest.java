package com.hls.streaming.unit.media.service;

import com.hls.streaming.common.enums.ErrorEnum;
import com.hls.streaming.infrastructure.config.error.ErrorCodeMessage;
import com.hls.streaming.infrastructure.storage.S3Client;
import com.hls.streaming.infrastructure.config.error.ErrorCodeConfig;
import com.hls.streaming.infrastructure.config.properties.StorageConfig;
import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.media.domain.enums.VideoStatus;
import com.hls.streaming.media.domain.repository.VideoRepository;
import com.hls.streaming.media.dto.*;
import com.hls.streaming.media.event.OnUploadVideoEvent;
import com.hls.streaming.media.service.multipart.MultipartService;
import com.hls.streaming.media.service.processing.ffmpeg.FfmpegProcessRegistry;
import com.hls.streaming.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultipartServiceTest {

    private MultipartService service;

    @Mock
    private S3Client s3Client;

    @Mock
    private StorageConfig storageConfig;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private FfmpegProcessRegistry processRegistry;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ErrorCodeConfig errorCodeConfig;

    @BeforeEach
    void setUp() {
        service = new MultipartService(s3Client, storageConfig, videoRepository, processRegistry, eventPublisher, errorCodeConfig);
    }

    @Test
    void initMultipartUpload_ShouldReturnKeyAndUploadId() {
        // Given
        String userId = "user-id-123";
        String fileName = "test-video.mp4";
        String contentType = "video/mp4";
        String uploadId = "upload-id-123";

        when(storageConfig.getRawVideoPrefix()).thenReturn("raw-videos/");
        when(s3Client.createMultipartUpload(any(), eq(contentType))).thenReturn(uploadId);

        // When
        MultipartInitResponse response = service.initMultipartUpload(userId, fileName, contentType);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUploadId()).isEqualTo(uploadId);
        assertThat(response.getKey()).contains(userId);

        verify(s3Client).createMultipartUpload(any(), eq(contentType));
    }

    @Test
    void getUploadPartUrl_ShouldReturnPresignedUrl() {
        // Given
        String key = "raw-videos/user-id-123/video.mp4";
        String uploadId = "upload-id-123";
        int partNumber = 1;
        String presignedUrl = "https://s3.example.com/presigned-url";

        when(s3Client.generatePresignedUploadPartUrl(key, uploadId, partNumber))
                .thenReturn(presignedUrl);

        // When
        MultipartUploadUrlResponse response = service.getUploadPartUrl(key, uploadId, partNumber);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUrl()).isEqualTo(presignedUrl);

        verify(s3Client).generatePresignedUploadPartUrl(key, uploadId, partNumber);
    }

    @Test
    void completeMultipartUpload_WithValidRequest_ShouldSaveVideoAndPublishEvent() {
        // Given
        String userId = "user-id-123";
        String key = "raw-videos/user-id-123/test-video.mp4";
        String uploadId = "upload-id-123";

        CompleteMultipartRequest.Part part1 = CompleteMultipartRequest.Part.builder()
                .partNumber(1)
                .etag("etag-1")
                .build();

        CompleteMultipartRequest request = CompleteMultipartRequest.builder()
                .key(key)
                .uploadId(uploadId)
                .title("Test Video")
                .description("Test Description")
                .contentType("video/mp4")
                .size(1024000L)
                .parts(List.of(part1))
                .build();

        Video savedVideo = Video.builder()
                .id("video-id-123")
                .userId(userId)
                .title("Test Video")
                .status(VideoStatus.CREATED)
                .build();

        when(videoRepository.save(any(Video.class))).thenReturn(savedVideo);

        // When
        VideoUploadResponse response = service.completeMultipartUpload(userId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getVideoId()).isEqualTo("video-id-123");
        assertThat(response.getStatus()).isEqualTo(VideoStatus.CREATED);

        verify(s3Client).completeMultipartUpload(eq(key), eq(uploadId), any());
        verify(videoRepository).save(any(Video.class));
        verify(eventPublisher).publishEvent(any(OnUploadVideoEvent.class));
    }

    @Test
    void completeMultipartUpload_WithFileTooLarge_ShouldThrowException() {
        // Given
        String userId = "user-id-123";

        CompleteMultipartRequest request = CompleteMultipartRequest.builder()
                .key("raw-videos/user-id-123/large-video.mp4")
                .uploadId("upload-id-123")
                .size(3L * 1024 * 1024 * 1024) // 3GB, exceeds 2GB limit
                .parts(List.of())
                .build();

        // When & Then
        assertThatThrownBy(() -> service.completeMultipartUpload(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File too large");

        verify(videoRepository, never()).save(any());
    }

    @Test
    void completeMultipartUpload_WithInvalidKey_ShouldThrowException() {
        // Given
        String userId = "user-id-123";

        CompleteMultipartRequest request = CompleteMultipartRequest.builder()
                .key("raw-videos/different-user/video.mp4")
                .uploadId("upload-id-123")
                .size(1024000L)
                .parts(List.of())
                .build();

        // When & Then
        assertThatThrownBy(() -> service.completeMultipartUpload(userId, request))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Invalid key");

        verify(videoRepository, never()).save(any());
    }

    @Test
    void abortMultipartUpload_WithValidVideo_ShouldAbortAndCancel() {
        // Given
        String userId = "user-id-123";
        String key = "raw-videos/user-id-123/video.mp4";
        String uploadId = "upload-id-123";

        Video video = Video.builder()
                .id("video-id-123")
                .userId(userId)
                .objectKey(key)
                .build();

        AbortUploadVideoRequest request = AbortUploadVideoRequest.builder()
                .key(key)
                .uploadId(uploadId)
                .userId(userId)
                .build();

        when(videoRepository.findVideoByObjectKeyAndUserId(key, userId))
                .thenReturn(Optional.of(video));

        // When
        service.abortMultipartUpload(request);

        // Then
        verify(s3Client).abortMultipartUpload(key, uploadId);
        verify(processRegistry).cancel(key);
    }

    @Test
    void abortMultipartUpload_WithNonexistentVideo_ShouldThrowNotFoundException() {
        // Given
        String userId = "user-id-123";
        String key = "raw-videos/user-id-123/nonexistent.mp4";
        String uploadId = "upload-id-123";

        AbortUploadVideoRequest request = AbortUploadVideoRequest.builder()
                .key(key)
                .uploadId(uploadId)
                .userId(userId)
                .build();

        when(videoRepository.findVideoByObjectKeyAndUserId(key, userId))
                .thenReturn(Optional.empty());
        when(errorCodeConfig.getMessage(eq(1000400101L)))
                .thenReturn(ErrorCodeMessage.builder()
                        .code(1000400101L)
                        .message("Video not found")
                        .build());
        // When & Then
        assertThatThrownBy(() -> service.abortMultipartUpload(request))
                .isInstanceOf(NotFoundException.class);

        verify(s3Client, never()).abortMultipartUpload(any(), any());
    }

    @Test
    void completeMultipartUpload_ShouldExtractFolderAndFileNameFromKey() {
        // Given
        String userId = "user-id-123";
        String key = "raw-videos/user-id-123/subfolder/test-video.mp4";
        String uploadId = "upload-id-123";

        CompleteMultipartRequest request = CompleteMultipartRequest.builder()
                .key(key)
                .uploadId(uploadId)
                .title("Test")
                .size(1024000L)
                .contentType("video/mp4")
                .parts(List.of())
                .build();

        Video savedVideo = Video.builder()
                .id("video-id-123")
                .status(VideoStatus.CREATED)
                .build();

        when(videoRepository.save(any(Video.class))).thenReturn(savedVideo);

        // When
        service.completeMultipartUpload(userId, request);

        // Then
        ArgumentCaptor<Video> videoCaptor = ArgumentCaptor.forClass(Video.class);
        verify(videoRepository).save(videoCaptor.capture());

        Video savedArgument = videoCaptor.getValue();
        assertThat(savedArgument.getFileName()).isEqualTo("test-video.mp4");
        assertThat(savedArgument.getFolder()).contains("raw-videos");
    }
}
