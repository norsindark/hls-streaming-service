package com.hls.streaming.unit.media.controller;

import com.hls.streaming.common.dtos.PageResponse;
import com.hls.streaming.media.controller.VideoProcessingController;
import com.hls.streaming.media.controller.VideoPublicController;
import com.hls.streaming.media.dto.*;
import com.hls.streaming.media.service.multipart.MultipartService;
import com.hls.streaming.media.service.query.VideoQueryService;
import com.hls.streaming.media.service.upload.VideoUploadService;
import com.hls.streaming.security.context.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoControllerTest {

    @Mock
    private VideoUploadService videoUploadService;

    @Mock
    private MultipartService multipartService;

    @Mock
    private VideoQueryService videoQueryService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private VideoProcessingController videoProcessingController;

    @InjectMocks
    private VideoPublicController videoPublicController;

    @BeforeEach
    void setUp() {
        // CurrentUserProvider is now injected via @InjectMocks
    }

    @Test
    void getVideoById_ShouldReturnVideoResponse() {
        String videoId = "video-id-123";

        VideoResponse expectedResponse = VideoResponse.builder()
                .videoId(videoId)
                .title("Test Video")
                .build();

        when(videoQueryService.getVideoById(videoId)).thenReturn(expectedResponse);

        VideoResponse response = videoPublicController.getVideoById(videoId);

        assertThat(response).isNotNull();
        assertThat(response.getVideoId()).isEqualTo(videoId);

        verify(videoQueryService).getVideoById(videoId);
    }

    @Test
    void getMyVideos_ShouldReturnUserVideos() {
        String userId = "user-id-123";
        Pageable pageable = PageRequest.of(0, 10);

        when(currentUserProvider.getUserId()).thenReturn(userId);

        PageResponse<VideoResponse> expectedResponse = PageResponse.<VideoResponse>builder()
                .totalElements(2)
                .totalPages(1)
                .content(List.of(
                        VideoResponse.builder().videoId("video-1").build(),
                        VideoResponse.builder().videoId("video-2").build()
                ))
                .build();

        when(videoQueryService.getVideosByUser(userId, pageable)).thenReturn(expectedResponse);

        PageResponse<VideoResponse> response = videoPublicController.getMyVideos(pageable);

        assertThat(response.getTotalElements()).isEqualTo(2);

        verify(videoQueryService).getVideosByUser(userId, pageable);
    }

    @Test
    void getVideosByUserId_ShouldReturnVideosByUser() {
        String userId = "user-id-123";
        Pageable pageable = PageRequest.of(0, 10);

        PageResponse<VideoResponse> expectedResponse = PageResponse.<VideoResponse>builder()
                .totalElements(1)
                .content(List.of(VideoResponse.builder().videoId("video-1").build()))
                .build();

        when(videoQueryService.getVideosByUser(userId, pageable)).thenReturn(expectedResponse);

        PageResponse<VideoResponse> response = videoPublicController.getVideosByUserId(userId, pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);

        verify(videoQueryService).getVideosByUser(userId, pageable);
    }

    @Test
    void uploadVideo_WithValidFile_ShouldReturnUploadResponse() throws Exception {
        String userId = "user-id-123";
        String title = "New Video";
        String description = "Video Description";

        when(currentUserProvider.getUserId()).thenReturn(userId);
        when(multipartFile.getOriginalFilename()).thenReturn("test-video.mp4");
        when(multipartFile.isEmpty()).thenReturn(false);

        VideoUploadResponse expectedResponse = VideoUploadResponse.builder()
                .videoId("video-id-123")
                .build();

        when(videoUploadService.uploadRawVideo(eq(userId), eq(multipartFile), eq(title), eq(description), any()))
                .thenReturn(expectedResponse);

        VideoUploadResponse response = videoProcessingController.uploadVideo(multipartFile, title, description);

        assertThat(response.getVideoId()).isEqualTo("video-id-123");

        verify(videoUploadService).uploadRawVideo(eq(userId), eq(multipartFile), eq(title), eq(description), any());
    }

    @Test
    void initMultipartUpload_ShouldReturnUploadIdAndKey() {
        String userId = "user-id-123";
        String fileName = "large-video.mp4";
        String contentType = "video/mp4";

        when(currentUserProvider.getUserId()).thenReturn(userId);

        MultipartInitResponse expectedResponse = MultipartInitResponse.builder()
                .key("raw-videos/" + userId + "/large-video.mp4")
                .uploadId("upload-id-123")
                .build();

        when(multipartService.initMultipartUpload(userId, fileName, contentType))
                .thenReturn(expectedResponse);

        MultipartInitResponse response = videoProcessingController.initMultipartUpload(fileName, contentType);

        assertThat(response.getUploadId()).isEqualTo("upload-id-123");

        verify(multipartService).initMultipartUpload(userId, fileName, contentType);
    }

    @Test
    void getUploadPartUrl_ShouldReturnPresignedUrl() {
        String key = "raw-videos/user-id-123/video.mp4";
        String uploadId = "upload-id-123";
        int partNumber = 1;

        MultipartUploadUrlResponse expectedResponse = MultipartUploadUrlResponse.builder()
                .url("https://s3.example.com/presigned-url")
                .build();

        when(multipartService.getUploadPartUrl(key, uploadId, partNumber))
                .thenReturn(expectedResponse);

        MultipartUploadUrlResponse response = videoProcessingController.getUploadPartUrl(key, uploadId, partNumber);

        assertThat(response.getUrl()).contains("s3.example.com");

        verify(multipartService).getUploadPartUrl(key, uploadId, partNumber);
    }

    @Test
    void completeMultipartUpload_ShouldReturnUploadResponse() {
        String userId = "user-id-123";

        when(currentUserProvider.getUserId()).thenReturn(userId);

        CompleteMultipartRequest request = CompleteMultipartRequest.builder()
                .key("raw-videos/" + userId + "/video.mp4")
                .uploadId("upload-id-123")
                .title("Complete Video")
                .build();

        VideoUploadResponse expectedResponse = VideoUploadResponse.builder()
                .videoId("video-id-123")
                .build();

        when(multipartService.completeMultipartUpload(userId, request))
                .thenReturn(expectedResponse);

        VideoUploadResponse response = videoProcessingController.completeMultipartUpload(request);

        assertThat(response.getVideoId()).isEqualTo("video-id-123");

        verify(multipartService).completeMultipartUpload(userId, request);
    }

    @Test
    void abortMultipartUpload_ShouldCallAbortService() {
        String userId = "user-id-123";

        when(currentUserProvider.getUserId()).thenReturn(userId);

        AbortUploadVideoRequest request = AbortUploadVideoRequest.builder()
                .key("raw-videos/" + userId + "/video.mp4")
                .uploadId("upload-id-123")
                .build();

        videoProcessingController.abortMultipartUpload(request);

        assertThat(request.getUserId()).isEqualTo(userId);

        verify(multipartService).abortMultipartUpload(request);
    }
}
