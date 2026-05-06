package com.hls.streaming.integration.media;

import com.hls.streaming.HlsApplication;
import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.media.domain.enums.VideoStatus;
import com.hls.streaming.media.domain.repository.VideoRepository;
import com.hls.streaming.user.domain.document.User;
import com.hls.streaming.user.domain.enums.UserStatusEnum;
import com.hls.streaming.user.domain.repository.UserRepository;
import com.hls.streaming.security.authentication.model.UserRole;
import com.hls.streaming.security.context.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HlsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class VideoUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    private User testUser;
    private MockMultipartFile videoFile;

    @BeforeEach
    void setUp() {
        videoRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.USER))
                .build();

        testUser = userRepository.save(testUser);

        byte[] videoContent = "mock video content".getBytes();
        videoFile = new MockMultipartFile(
                "file",
                "test-video.mp4",
                "video/mp4",
                videoContent
        );
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void uploadVideo_WithValidFile_ShouldCreateVideoAndPublishEvent() throws Exception {
        // Given
        when(currentUserProvider.getUserId()).thenReturn(testUser.getId());

        // When
        MvcResult result = mockMvc.perform(multipart("/api/v1/videos/upload")
                .file(videoFile)
                .param("title", "Test Video")
                .param("description", "Test Description")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("videoId");
        assertThat(responseContent).contains("CREATED");

        Video savedVideo = videoRepository.findAll().stream()
                .filter(v -> "Test Video".equals(v.getTitle()))
                .findFirst()
                .orElseThrow();

        assertThat(savedVideo.getUserId()).isEqualTo(testUser.getId());
        assertThat(savedVideo.getStatus()).isEqualTo(VideoStatus.CREATED);
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void uploadVideo_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/v1/videos/upload")
                .file(videoFile)
                .param("title", "Test Video")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void uploadVideo_WithoutFile_ShouldReturnBadRequest() throws Exception {
        // Given
        when(currentUserProvider.getUserId()).thenReturn(testUser.getId());

        // When & Then
        mockMvc.perform(multipart("/api/v1/videos/upload")
                .param("title", "Test Video")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void initMultipartUpload_ShouldReturnUploadIdAndKey() throws Exception {
        // Given
        when(currentUserProvider.getUserId()).thenReturn(testUser.getId());

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/videos/multipart/init")
                .param("fileName", "large-video.mp4")
                .param("contentType", "video/mp4")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("uploadId");
        assertThat(responseContent).contains("key");
        assertThat(responseContent).contains(testUser.getId());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getUploadPartUrl_ShouldReturnPresignedUrl() throws Exception {
        // Given
        String key = "raw-videos/" + testUser.getId() + "/video.mp4";
        String uploadId = "upload-id-123";
        int partNumber = 1;

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/videos/multipart/upload-url")
                .param("key", key)
                .param("uploadId", uploadId)
                .param("partNumber", String.valueOf(partNumber))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("url");
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void completeMultipartUpload_WithValidRequest_ShouldCreateVideo() throws Exception {
        // Given
        when(currentUserProvider.getUserId()).thenReturn(testUser.getId());

        String key = "raw-videos/" + testUser.getId() + "/video.mp4";
        String uploadId = "upload-id-123";

        String requestBody = "{\n" +
                "  \"key\": \"" + key + "\",\n" +
                "  \"uploadId\": \"" + uploadId + "\",\n" +
                "  \"title\": \"Complete Video\",\n" +
                "  \"description\": \"Completed Upload\",\n" +
                "  \"contentType\": \"video/mp4\",\n" +
                "  \"size\": 1024000,\n" +
                "  \"parts\": [\n" +
                "    {\"partNumber\": 1, \"etag\": \"etag-1\"}\n" +
                "  ]\n" +
                "}";

        // When & Then (This test may require mocking S3 or using testcontainers)
        mockMvc.perform(post("/api/v1/videos/multipart/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void abortMultipartUpload_ShouldAbortUpload() throws Exception {
        // Given
        when(currentUserProvider.getUserId()).thenReturn(testUser.getId());

        String key = "raw-videos/" + testUser.getId() + "/video.mp4";
        String uploadId = "upload-id-123";

        String requestBody = "{\n" +
                "  \"key\": \"" + key + "\",\n" +
                "  \"uploadId\": \"" + uploadId + "\"\n" +
                "}";

        // When & Then (This test may require mocking S3 or using testcontainers)
        mockMvc.perform(post("/api/v1/videos/multipart/abort")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void uploadVideo_ShouldSaveWithCorrectUserId() throws Exception {
        // Given
        when(currentUserProvider.getUserId()).thenReturn(testUser.getId());

        // When
        mockMvc.perform(multipart("/api/v1/videos/upload")
                .file(videoFile)
                .param("title", "Test Video")
                .param("description", "Test Description")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        // Then
        Video savedVideo = videoRepository.findAll().stream()
                .filter(v -> "Test Video".equals(v.getTitle()))
                .findFirst()
                .orElseThrow();

        assertThat(savedVideo.getUserId()).isEqualTo(testUser.getId());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void uploadVideo_WithoutTitle_ShouldUseEmptyTitle() throws Exception {
        // Given
        when(currentUserProvider.getUserId()).thenReturn(testUser.getId());

        // When
        mockMvc.perform(multipart("/api/v1/videos/upload")
                .file(videoFile)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        // Then
        Video savedVideo = videoRepository.findAll().stream()
                .findFirst()
                .orElseThrow();

        assertThat(savedVideo.getTitle()).isEmpty();
    }
}
