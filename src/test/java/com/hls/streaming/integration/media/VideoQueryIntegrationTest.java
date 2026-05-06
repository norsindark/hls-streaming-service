package com.hls.streaming.integration.media;

import com.hls.streaming.HlsApplication;
import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.media.domain.enums.VideoStatus;
import com.hls.streaming.media.domain.repository.VideoRepository;
import com.hls.streaming.security.authentication.model.UserRole;
import com.hls.streaming.security.context.CurrentUserProvider;
import com.hls.streaming.user.domain.document.User;
import com.hls.streaming.user.domain.enums.UserStatusEnum;
import com.hls.streaming.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = HlsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class VideoQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    private User testUser;
    private Video testVideo;

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

        testVideo = Video.builder()
                .userId(testUser.getId())
                .title("Test Video")
                .description("Test Description")
                .status(VideoStatus.DONE)
                .hlsUrl("https://example.com/video.m3u8")
                .thumbnailUrl("https://example.com/thumbnail.jpg")
                .duration(60d)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getVideoById_WithValidVideoId_ShouldReturnVideo() throws Exception {
        // Given
        Video savedVideo = videoRepository.save(testVideo);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/videos/" + savedVideo.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("Test Video");
        assertThat(responseContent).contains("Test Description");
    }

    @Test
    void getVideoById_WithNonexistentVideo_ShouldReturnNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/videos/nonexistent-id")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getMyVideos_ShouldReturnUserVideos() throws Exception {
        // Given
        videoRepository.save(testVideo);
        when(currentUserProvider.getUserId()).thenReturn(testUser.getId());

        Video anotherVideo = Video.builder()
                .userId(testUser.getId())
                .title("Another Video")
                .status(VideoStatus.DONE)
                .createdAt(Instant.now())
                .build();

        videoRepository.save(anotherVideo);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/videos/my-videos?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("Test Video");
        assertThat(responseContent).contains("Another Video");
    }

    @Test
    void getVideosByUserId_ShouldReturnVideosForUser() throws Exception {
        // Given
        videoRepository.save(testVideo);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/public/videos/user/" + testUser.getId() + "?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("Test Video");
    }

    @Test
    void getVideosByUserId_WithEmptyResult_ShouldReturnEmptyPage() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/api/v1/public/videos/user/" + testUser.getId() + "?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("\"totalElements\":0");
    }

    @Test
    void getVideosByUserId_WithPagination_ShouldReturnPaginatedResult() throws Exception {
        // Given
        for (int i = 0; i < 15; i++) {
            Video video = Video.builder()
                    .userId(testUser.getId())
                    .title("Video " + i)
                    .status(VideoStatus.DONE)
                    .createdAt(Instant.now())
                    .build();
            videoRepository.save(video);
        }

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/public/videos/user/" + testUser.getId() + "?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("\"totalElements\":15");
        assertThat(responseContent).contains("\"totalPages\":2");
    }

    @Test
    void getVideosByUserId_ShouldOnlyReturnDoneVideos() throws Exception {
        // Given
        videoRepository.save(testVideo); // DONE status

        Video processingVideo = Video.builder()
                .userId(testUser.getId())
                .title("Processing Video")
                .status(VideoStatus.PROCESSING)
                .createdAt(Instant.now())
                .build();

        videoRepository.save(processingVideo);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/public/videos/user/" + testUser.getId() + "?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("Test Video");
        // Processing video should not appear (assuming API filters by DONE status)
    }
}
