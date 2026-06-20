package com.hls.streaming.unit.media.service;

import com.hls.streaming.infrastructure.storage.S3Client;
import com.hls.streaming.infrastructure.config.properties.StorageConfig;
import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.media.domain.enums.VideoStatus;
import com.hls.streaming.media.domain.repository.VideoRepository;
import com.hls.streaming.media.dto.VideoUploadResponse;
import com.hls.streaming.media.event.OnUploadVideoEvent;
import com.hls.streaming.media.service.upload.VideoUploadService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoUploadServiceTest {

    private VideoUploadService service;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private S3Client s3Client;

    @Mock
    private StorageConfig storageConfig;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MultipartFile multipartFile;

    @Mock
    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        service = new VideoUploadService(videoRepository, s3Client, storageConfig, eventPublisher, registry);
    }

    @Test
    void uploadRawVideo_WithValidFile_ShouldSaveVideoAndPublishEvent() throws Exception {
        // Given
        String userId = "user-id-123";
        String fileName = "test-video.mp4";
        String title = "Test Video";
        String description = "Test Description";
        byte[] fileContent = "video content".getBytes();

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));
        when(multipartFile.getContentType()).thenReturn("video/mp4");
        when(multipartFile.getSize()).thenReturn((long) fileContent.length);

        when(storageConfig.getRawVideoPrefix()).thenReturn("raw-videos/");

        Video savedVideo = Video.builder()
                .id("video-id-123")
                .userId(userId)
                .title(title)
                .description(description)
                .fileName(fileName)
                .status(VideoStatus.CREATED)
                .build();

        when(videoRepository.save(any(Video.class))).thenReturn(savedVideo);

        // When
        VideoUploadResponse response = service.uploadRawVideo(userId, multipartFile, title, description, fileName);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getVideoId()).isEqualTo("video-id-123");
        assertThat(response.getStatus()).isEqualTo(VideoStatus.CREATED);

        verify(s3Client).uploadFile(
                contains(userId),
                eq(fileName),
                any(InputStream.class),
                eq("video/mp4"),
                eq((long) fileContent.length)
        );
        verify(videoRepository).save(any(Video.class));
        verify(eventPublisher).publishEvent(any(OnUploadVideoEvent.class));
    }

    @Test
    void uploadRawVideo_WithDefaultContentType_ShouldUseMp4() throws Exception {
        // Given
        String userId = "user-id-123";
        String fileName = "test-video";
        byte[] fileContent = "content".getBytes();

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));
        when(multipartFile.getContentType()).thenReturn(null);
        when(multipartFile.getSize()).thenReturn((long) fileContent.length);

        when(storageConfig.getRawVideoPrefix()).thenReturn("raw-videos/");

        Video savedVideo = Video.builder()
                .id("video-id-123")
                .userId(userId)
                .fileName(fileName)
                .status(VideoStatus.CREATED)
                .build();

        when(videoRepository.save(any(Video.class))).thenReturn(savedVideo);

        // When
        service.uploadRawVideo(userId, multipartFile, "Title", "Description", fileName);

        // Then
        verify(s3Client).uploadFile(
                anyString(),
                anyString(),
                any(InputStream.class),
                eq("video/mp4"),
                anyLong());
    }

    @Test
    void uploadRawVideo_WithEmptyFile_ShouldThrowException() {
        // Given
        String userId = "user-id-123";
        String fileName = "test-video.mp4";

        when(multipartFile.isEmpty()).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> service.uploadRawVideo(userId, multipartFile, "Title", "Description", fileName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid file");

        verify(videoRepository).save(any(Video.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void uploadRawVideo_WithNullFile_ShouldThrowException() {
        // Given
        String userId = "user-id-123";

        // When & Then
        assertThatThrownBy(() ->
                service.uploadRawVideo(userId, null, "Title", "Description", "test.mp4")
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid file");

        verify(videoRepository, never()).save(any());
    }

    @Test
    void uploadRawVideo_WithS3UploadFailure_ShouldSaveVideoWithFailedStatusAndThrow() throws Exception {
        // Given
        String userId = "user-id-123";
        String fileName = "test-video.mp4";
        byte[] fileContent = "content".getBytes();

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));
        when(multipartFile.getContentType()).thenReturn("video/mp4");
        when(multipartFile.getSize()).thenReturn((long) fileContent.length);

        when(storageConfig.getRawVideoPrefix()).thenReturn("raw-videos/");
        when(s3Client.uploadFile(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("S3 upload failed"));

        Video failedVideo = Video.builder()
                .status(VideoStatus.FAILED)
                .build();

        when(videoRepository.save(any(Video.class))).thenReturn(failedVideo);

        // When & Then
        assertThatThrownBy(() ->
                service.uploadRawVideo(userId, multipartFile, "Title", "Description", fileName)
        ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Upload failed");

        ArgumentCaptor<Video> videoCaptor = ArgumentCaptor.forClass(Video.class);
        verify(videoRepository, atLeastOnce()).save(videoCaptor.capture());

        Video savedVideo = videoCaptor.getValue();
        assertThat(savedVideo.getStatus()).isEqualTo(VideoStatus.FAILED);
    }

    @Test
    void uploadRawVideo_ShouldPublishEventWithVideoId() throws Exception {
        // Given
        String userId = "user-id-123";
        String fileName = "test-video.mp4";
        String videoId = "video-id-123";
        byte[] fileContent = "content".getBytes();

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));
        when(multipartFile.getContentType()).thenReturn("video/mp4");
        when(multipartFile.getSize()).thenReturn((long) fileContent.length);

        when(storageConfig.getRawVideoPrefix()).thenReturn("raw-videos/");

        Video savedVideo = Video.builder()
                .id(videoId)
                .userId(userId)
                .status(VideoStatus.CREATED)
                .build();

        when(videoRepository.save(any(Video.class))).thenReturn(savedVideo);

        // When
        service.uploadRawVideo(userId, multipartFile, "Title", "Description", fileName);

        // Then
        ArgumentCaptor<OnUploadVideoEvent> eventCaptor = ArgumentCaptor.forClass(OnUploadVideoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        OnUploadVideoEvent event = eventCaptor.getValue();
        assertThat(event.getVideoId()).isEqualTo(videoId);
    }
}
