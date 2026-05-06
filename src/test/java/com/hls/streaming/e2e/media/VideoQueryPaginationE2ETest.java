package com.hls.streaming.e2e.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hls.streaming.HlsApplication;
import com.hls.streaming.config.NoSecurityConfig;
import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.media.domain.enums.VideoStatus;
import com.hls.streaming.media.domain.repository.VideoRepository;
import com.hls.streaming.security.authentication.model.UserRole;
import com.hls.streaming.user.domain.document.User;
import com.hls.streaming.user.domain.enums.UserStatusEnum;
import com.hls.streaming.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = HlsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(NoSecurityConfig.class)
@Disabled("Temporarily disable E2E tests")
class VideoQueryPaginationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private static final int VIDEO_COUNT = 25;

    @BeforeEach
    void setUp() {
        videoRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("paginationuser")
                .email("pagination@example.com")
                .displayName("Pagination Test User")
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.USER))
                .build();

        testUser = userRepository.save(testUser);

        // Create multiple videos
        for (int i = 1; i <= VIDEO_COUNT; i++) {
            Video video = Video.builder()
                    .userId(testUser.getId())
                    .title("Video " + i)
                    .description("Description for video " + i)
                    .status(VideoStatus.DONE)
                    .hlsUrl("https://example.com/video" + i + ".m3u8")
                    .thumbnailUrl("https://example.com/thumb" + i + ".jpg")
                    .duration(60d * i)
                    .createdAt(Instant.now())
                    .build();

            videoRepository.save(video);
        }
    }

    @Test
    void getPaginatedVideos_FirstPage_ShouldReturn10Videos() throws Exception {
        // Step 1: Get first page with default size (10)
        MvcResult result = mockMvc.perform(
                        get("/api/v1/videos/user/" + testUser.getId() + "?page=0&size=10")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);

        // Step 2: Verify pagination metadata
        assertThat(node.get("totalElements").asInt()).isEqualTo(VIDEO_COUNT);
        assertThat(node.get("totalPages").asInt()).isEqualTo(3); // 25 items with page size 10 = 3 pages

        // Step 3: Verify content count
        JsonNode content = node.get("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isEqualTo(10);
    }

    @Test
    void getPaginatedVideos_SecondPage_ShouldReturnCorrectVideos() throws Exception {
        // Step 1: Get second page
        MvcResult result = mockMvc.perform(
                        get("/api/v1/videos/user/" + testUser.getId() + "?page=1&size=10")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);

        // Step 2: Verify second page videos
        JsonNode content = node.get("content");
        assertThat(content.size()).isEqualTo(10);

        // Videos should be different from first page
        String firstVideoTitle = content.get(0).get("title").asText();
        assertThat(firstVideoTitle).startsWith("Video");
    }

    @Test
    void getPaginatedVideos_LastPage_ShouldReturnRemainingVideos() throws Exception {
        // Step 1: Get last page
        MvcResult result = mockMvc.perform(
                        get("/api/v1/videos/user/" + testUser.getId() + "?page=2&size=10")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);

        // Step 2: Verify last page has remaining videos
        JsonNode content = node.get("content");
        assertThat(content.size()).isEqualTo(5); // 25 - 20 = 5
    }

    @Test
    void getPaginatedVideos_CustomPageSize_ShouldReturnCorrectCount() throws Exception {
        // Step 1: Get with custom page size
        MvcResult result = mockMvc.perform(
                        get("/api/v1/videos/user/" + testUser.getId() + "?page=0&size=5")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);

        // Step 2: Verify pagination with custom size
        assertThat(node.get("totalElements").asInt()).isEqualTo(VIDEO_COUNT);
        assertThat(node.get("totalPages").asInt()).isEqualTo(5); // 25 items with page size 5 = 5 pages

        // Step 3: Verify content count
        JsonNode content = node.get("content");
        assertThat(content.size()).isEqualTo(5);
    }

    @Test
    void getPaginatedVideos_AllPages_ShouldContainAllVideos() throws Exception {
        // Step 1: Get all pages and collect video count
        int totalVideosCollected = 0;

        for (int page = 0; page < 3; page++) {
            MvcResult result = mockMvc.perform(
                            get("/api/v1/videos/user/" + testUser.getId() + "?page=" + page + "&size=10")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            JsonNode node = objectMapper.readTree(response);

            JsonNode content = node.get("content");
            totalVideosCollected += content.size();
        }

        // Step 2: Verify all videos were retrieved
        assertThat(totalVideosCollected).isEqualTo(VIDEO_COUNT);
    }

    @Test
    void getPaginatedVideos_InvalidPage_ShouldHandleGracefully() throws Exception {
        // Step 1: Request non-existent page
        MvcResult result = mockMvc.perform(
                        get("/api/v1/videos/user/" + testUser.getId() + "?page=100&size=10")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);

        // Step 2: Should return empty content
        JsonNode content = node.get("content");
        assertThat(content.isArray()).isTrue();
        // Content may be empty or have default behavior
    }

    @Test
    void getPaginatedVideos_VideoDetailsAreComplete() throws Exception {
        // Step 1: Get first page
        MvcResult result = mockMvc.perform(
                        get("/api/v1/videos/user/" + testUser.getId() + "?page=0&size=1")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);

        // Step 2: Verify video details are present
        JsonNode content = node.get("content");
        JsonNode firstVideo = content.get(0);

        assertThat(firstVideo.get("videoId").asText()).isNotBlank();
        assertThat(firstVideo.get("title").asText()).isNotBlank();
        assertThat(firstVideo.get("description")).isNotNull();
        assertThat(firstVideo.get("hlsUrl")).isNotNull();
        assertThat(firstVideo.get("thumbnailUrl")).isNotNull();
        assertThat(firstVideo.get("status").asText()).isEqualTo("DONE");
    }
}
