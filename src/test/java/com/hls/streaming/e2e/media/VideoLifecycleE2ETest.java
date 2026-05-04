package com.hls.streaming.e2e.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hls.streaming.HlsApplication;
import com.hls.streaming.config.NoSecurityConfig;
import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.media.domain.enums.VideoStatus;
import com.hls.streaming.media.domain.repository.VideoRepository;
import com.hls.streaming.security.authentication.model.UserRole;
import com.hls.streaming.security.constants.SecurityConstant;
import com.hls.streaming.security.context.CurrentUserProvider;
import com.hls.streaming.user.domain.document.User;
import com.hls.streaming.user.domain.enums.UserStatusEnum;
import com.hls.streaming.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = HlsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(NoSecurityConfig.class)
@Disabled("Temporarily disable E2E tests")
class VideoLifecycleE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
                .username("videotestuser")
                .email("videotest@example.com")
                .displayName("Video Test User")
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.USER))
                .build();

        testUser = userRepository.save(testUser);

        Mockito.when(currentUserProvider.getUserId()).thenReturn(testUser.getId());

        byte[] videoContent = "mock video content for testing".getBytes();
        videoFile = new MockMultipartFile(
                "file",
                "test-video.mp4",
                "video/mp4",
                videoContent
        );
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void completeVideoUploadFlow_ShouldSucceedEndToEnd() throws Exception {
        // Step 1: Upload video file
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/videos/upload")
                        .file(videoFile)
                        .param("title", "E2E Test Video")
                        .param("description", "End-to-End Video Upload Test")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        String uploadResponse = uploadResult.getResponse().getContentAsString();
        JsonNode uploadNode = objectMapper.readTree(uploadResponse);

        String videoId = uploadNode.get("videoId").asText();
        String status = uploadNode.get("status").asText();

        assertThat(videoId).isNotBlank();
        assertThat(status).isEqualTo("CREATED");

        // Step 2: Verify video was created in database
        Video uploadedVideo = videoRepository.findAll().stream()
                .filter(v -> videoId.equals(v.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(uploadedVideo.getUserId()).isEqualTo(testUser.getId());
        assertThat(uploadedVideo.getTitle()).isEqualTo("E2E Test Video");
        assertThat(uploadedVideo.getDescription()).isEqualTo("End-to-End Video Upload Test");

        // Step 3: Retrieve uploaded video
        MvcResult getResult = mockMvc.perform(get("/api/v1/videos/" + videoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String getResponse = getResult.getResponse().getContentAsString();
        JsonNode getNode = objectMapper.readTree(getResponse);

        assertThat(getNode.get("videoId").asText()).isEqualTo(videoId);
        assertThat(getNode.get("title").asText()).isEqualTo("E2E Test Video");
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void userVideoListingFlow_ShouldReturnOnlyUserVideos() throws Exception {
        // Step 1: Upload first video
        MvcResult upload1Result = mockMvc.perform(multipart("/api/v1/videos/upload")
                        .file(videoFile)
                        .param("title", "User Video 1")
                        .param("description", "First Video")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        String upload1Response = upload1Result.getResponse().getContentAsString();
        JsonNode upload1Node = objectMapper.readTree(upload1Response);
        String videoId1 = upload1Node.get("videoId").asText();

        // Step 2: Upload second video
        MvcResult upload2Result = mockMvc.perform(multipart("/api/v1/videos/upload")
                        .file(videoFile)
                        .param("title", "User Video 2")
                        .param("description", "Second Video")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        String upload2Response = upload2Result.getResponse().getContentAsString();
        JsonNode upload2Node = objectMapper.readTree(upload2Response);
        String videoId2 = upload2Node.get("videoId").asText();

        // Step 3: Retrieve user's video list
        MvcResult listResult = mockMvc.perform(get("/api/v1/videos/my-videos?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String listResponse = listResult.getResponse().getContentAsString();
        JsonNode listNode = objectMapper.readTree(listResponse);

        assertThat(listNode.get("totalElements").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(listResponse).contains("User Video 1");
        assertThat(listResponse).contains("User Video 2");
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void multipleUsersVideoIsolation_ShouldIsolateDifferentUserVideos() throws Exception {
        // Create second user
        User secondUser = User.builder()
                .username("seconduser")
                .email("seconduser@example.com")
                .displayName("Second User")
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.USER))
                .build();

        secondUser = userRepository.save(secondUser);

        // Step 1: First user uploads video
        MvcResult upload1Result = mockMvc.perform(multipart("/api/v1/videos/upload")
                        .file(videoFile)
                        .param("title", "First User Video")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        String upload1Response = upload1Result.getResponse().getContentAsString();
        JsonNode upload1Node = objectMapper.readTree(upload1Response);
        String videoId1 = upload1Node.get("videoId").asText();

        // Step 2: Verify first user's videos
        MvcResult listResult = mockMvc.perform(get("/api/v1/videos/my-videos?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String listResponse = listResult.getResponse().getContentAsString();
        assertThat(listResponse).contains("First User Video");

        // Step 3: Retrieve first user's video count
        Video firstUserVideo = videoRepository.findAll().stream()
                .filter(v -> v.getUserId().equals(testUser.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(firstUserVideo.getUserId()).isEqualTo(testUser.getId());

        // Step 4: Get videos by second user ID (should be empty)
        MvcResult secondUserListResult = mockMvc.perform(
                        get("/api/v1/videos/user/" + secondUser.getId() + "?page=0&size=10")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String secondUserListResponse = secondUserListResult.getResponse().getContentAsString();
        JsonNode secondUserListNode = objectMapper.readTree(secondUserListResponse);

        assertThat(secondUserListNode.get("totalElements").asInt()).isEqualTo(0);
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void videoUploadWithoutTitleAndDescription_ShouldHaveDefaultValues() throws Exception {
        // Step 1: Upload video without title and description
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/videos/upload")
                        .file(videoFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        String uploadResponse = uploadResult.getResponse().getContentAsString();
        JsonNode uploadNode = objectMapper.readTree(uploadResponse);

        String videoId = uploadNode.get("videoId").asText();

        // Step 2: Verify video was created with default values
        Video uploadedVideo = videoRepository.findById(videoId).orElseThrow();

        assertThat(uploadedVideo.getTitle()).isEmpty();
        assertThat(uploadedVideo.getDescription()).isEmpty();
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getPublicUserVideos_ShouldReturnUserVideosList() throws Exception {
        // Setup: Create videos for test user
        for (int i = 1; i <= 5; i++) {
            Video video = Video.builder()
                    .userId(testUser.getId())
                    .title("Public Video " + i)
                    .status(VideoStatus.DONE)
                    .hlsUrl("https://example.com/video" + i + ".m3u8")
                    .createdAt(Instant.now())
                    .build();

            videoRepository.save(video);
        }

        // Step 1: Get public user videos
        MvcResult listResult = mockMvc.perform(
                        get("/api/v1/videos/user/" + testUser.getId() + "?page=0&size=10")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String listResponse = listResult.getResponse().getContentAsString();
        JsonNode listNode = objectMapper.readTree(listResponse);

        // Step 2: Verify pagination
        assertThat(listNode.get("totalElements").asInt()).isEqualTo(5);
        assertThat(listNode.get("totalPages").asInt()).isEqualTo(1);

        // Step 3: Verify video content
        assertThat(listResponse).contains("Public Video 1");
        assertThat(listResponse).contains("Public Video 5");
    }
}
