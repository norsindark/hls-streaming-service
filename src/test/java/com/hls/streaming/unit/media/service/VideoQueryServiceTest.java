package com.hls.streaming.unit.media.service;

import com.hls.streaming.infrastructure.config.error.ErrorCodeConfig;
import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.media.domain.enums.VideoStatus;
import com.hls.streaming.media.domain.repository.VideoRepository;
import com.hls.streaming.media.dto.VideoResponse;
import com.hls.streaming.media.mapper.VideoMapperFacade;
import com.hls.streaming.media.service.query.VideoQueryService;
import com.hls.streaming.common.dtos.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoQueryServiceTest {

    private VideoQueryService service;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private VideoMapperFacade videoMapperFacade;

    @Mock
    private ErrorCodeConfig errorCodeConfig;

    @BeforeEach
    void setUp() {
        service = new VideoQueryService(videoRepository, videoMapperFacade, errorCodeConfig);
    }

    @Test
    void getVideoById_WithValidVideoId_ShouldReturnVideoResponse() {
        // Given
        String videoId = "video-id-123";

        Video video = Video.builder()
                .id(videoId)
                .userId("user-id-123")
                .title("Test Video")
                .description("Test Description")
                .status(VideoStatus.DONE)
                .hlsUrl("https://example.com/video.m3u8")
                .thumbnailUrl("https://example.com/thumbnail.jpg")
                .createdAt(Instant.now())
                .build();

        VideoResponse expectedResponse = VideoResponse.builder()
                .videoId(videoId)
                .title("Test Video")
                .status(VideoStatus.DONE.toString())
                .build();

        when(videoRepository.findVideoByIdAndStatus(videoId, VideoStatus.DONE))
                .thenReturn(Optional.of(video));
        when(videoMapperFacade.toResponse(video)).thenReturn(expectedResponse);

        // When
        VideoResponse response = service.getVideoById(videoId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getVideoId()).isEqualTo(videoId);
        assertThat(response.getTitle()).isEqualTo("Test Video");

        verify(videoRepository).findVideoByIdAndStatus(videoId, VideoStatus.DONE);
        verify(videoMapperFacade).toResponse(video);
    }

    @Test
    void getVideoById_WithNonexistentVideo_ShouldThrowException() {
        // Given
        String videoId = "nonexistent-id";

        when(videoRepository.findVideoByIdAndStatus(videoId, VideoStatus.DONE))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.getVideoById(videoId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Video not found");

        verify(videoRepository).findVideoByIdAndStatus(videoId, VideoStatus.DONE);
        verify(videoMapperFacade, never()).toResponse(any());
    }

    @Test
    void getVideosByUser_WithValidUserId_ShouldReturnPagedResponse() {
        // Given
        String userId = "user-id-123";
        Pageable pageable = PageRequest.of(0, 10);

        Video video1 = Video.builder()
                .id("video-1")
                .userId(userId)
                .title("Video 1")
                .status(VideoStatus.DONE)
                .build();

        Video video2 = Video.builder()
                .id("video-2")
                .userId(userId)
                .title("Video 2")
                .status(VideoStatus.DONE)
                .build();

        VideoResponse response1 = VideoResponse.builder()
                .videoId("video-1")
                .title("Video 1")
                .build();

        VideoResponse response2 = VideoResponse.builder()
                .videoId("video-2")
                .title("Video 2")
                .build();

        when(videoRepository.findVideosByUserId(userId, pageable))
                .thenReturn(List.of(video1, video2));
        when(videoRepository.countVideosByUserId(userId)).thenReturn(20L);
        when(videoMapperFacade.toResponse(video1)).thenReturn(response1);
        when(videoMapperFacade.toResponse(video2)).thenReturn(response2);

        // When
        PageResponse<VideoResponse> response = service.getVideosByUser(userId, pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(20);
        assertThat(response.getTotalPages()).isEqualTo(2);

        verify(videoRepository).findVideosByUserId(userId, pageable);
        verify(videoRepository).countVideosByUserId(userId);
        verify(videoMapperFacade, times(2)).toResponse(any());
    }

    @Test
    void getVideosByUser_WithEmptyResult_ShouldReturnEmptyPageResponse() {
        // Given
        String userId = "user-id-123";
        Pageable pageable = PageRequest.of(0, 10);

        when(videoRepository.findVideosByUserId(userId, pageable))
                .thenReturn(List.of());
        when(videoRepository.countVideosByUserId(userId)).thenReturn(0L);

        // When
        PageResponse<VideoResponse> response = service.getVideosByUser(userId, pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getTotalPages()).isZero();

        verify(videoRepository).findVideosByUserId(userId, pageable);
        verify(videoRepository).countVideosByUserId(userId);
    }

    @Test
    void getVideosByUser_WithMultiplePages_ShouldCalculateTotalPagesCorrectly() {
        // Given
        String userId = "user-id-123";
        Pageable pageable = PageRequest.of(0, 10);

        when(videoRepository.findVideosByUserId(userId, pageable))
                .thenReturn(List.of());
        when(videoRepository.countVideosByUserId(userId)).thenReturn(25L);

        // When
        PageResponse<VideoResponse> response = service.getVideosByUser(userId, pageable);

        // Then
        assertThat(response.getTotalPages()).isEqualTo(3);
    }

    @Test
    void getVideosByUser_ShouldMapAllVideosCorrectly() {
        // Given
        String userId = "user-id-123";
        Pageable pageable = PageRequest.of(0, 5);

        Video video = Video.builder()
                .id("video-1")
                .userId(userId)
                .title("Test Video")
                .status(VideoStatus.DONE)
                .build();

        VideoResponse videoResponse = VideoResponse.builder()
                .videoId("video-1")
                .title("Test Video")
                .hlsUrl("https://example.com/hls.m3u8")
                .build();

        when(videoRepository.findVideosByUserId(userId, pageable))
                .thenReturn(List.of(video));
        when(videoRepository.countVideosByUserId(userId)).thenReturn(1L);
        when(videoMapperFacade.toResponse(video)).thenReturn(videoResponse);

        // When
        PageResponse<VideoResponse> response = service.getVideosByUser(userId, pageable);

        // Then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getHlsUrl()).isEqualTo("https://example.com/hls.m3u8");

        verify(videoMapperFacade).toResponse(video);
    }
}
