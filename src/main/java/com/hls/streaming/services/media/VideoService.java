package com.hls.streaming.services.media;

import com.hls.streaming.documents.media.Video;
import com.hls.streaming.dtos.PageResponse;
import com.hls.streaming.dtos.events.OnUploadVideoEvent;
import com.hls.streaming.dtos.media.*;
import com.hls.streaming.enums.UploadProcess;
import com.hls.streaming.features.mapper.video.VideoMapper;
import com.hls.streaming.features.mapper.video.VideoMapperFacade;
import com.hls.streaming.repositories.media.VideoRepository;
import com.hls.streaming.storage.S3Client;
import com.hls.streaming.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final FFMpegService ffMpegService;
    private final VideoRepository videoRepository;
    private final S3Client s3Client;
    private final ApplicationEventPublisher publisher;
    private final VideoMapperFacade videoMapperFacade;

    public VideoResponse getVideoById(final String videoId) {

        var video = videoRepository.findVideoByIdAndStatus(videoId, UploadProcess.DONE)
                .orElseThrow(() -> new IllegalStateException("Video not found or not ready"));
        return videoMapperFacade.toResponse(video);
    }

    public PageResponse<VideoResponse> getVideosByUser(final String userId, Pageable pageable) {

        var videos = videoRepository.findVideosByUserId(userId, pageable);
        var total = videoRepository.countVideosByUserId(userId);

        var content = videos.stream()
                .map(videoMapperFacade::toResponse)
                .toList();

        var totalPages = (long) Math.ceil((double) total / pageable.getPageSize());

        return PageResponse.<VideoResponse>builder()
                .totalElements(total)
                .totalPages(totalPages)
                .content(content)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public VideoUploadResponse processUploadRawVideo(
            final String userId,
            final MultipartFile file,
            final String title,
            final String description,
            final String fileName) {

        if (Objects.isNull(file) || file.isEmpty() || StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("Invalid file");
        }

        log.debug("File name: {}", fileName);

        var folder = "raw-videos/" + userId;
        var contentType = StringUtils.defaultIfBlank(file.getContentType(), "video/mp4");

        var document = Video.builder()
                .userId(userId)
                .title(StringUtils.defaultString(title))
                .description(StringUtils.defaultString(description))
                .folder(folder)
                .fileName(fileName)
                .contentType(contentType)
                .fileSize(file.getSize())
                .status(UploadProcess.CREATED)
                .build();

        try {
            s3Client.uploadFile(folder, fileName, file.getInputStream(), contentType, file.getSize());
        } catch (Exception e) {
            document.setStatus(UploadProcess.FAILED);
            videoRepository.save(document);
            throw new RuntimeException("Upload failed", e);
        }

        var saved = videoRepository.save(document);

        publisher.publishEvent(OnUploadVideoEvent.builder()
                .videoId(saved.getId())
                .build());

        return VideoUploadResponse.builder()
                .videoId(saved.getId())
                .status(saved.getStatus())
                .build();
    }

    public void processHlsConversion(final String videoId) {

        var video = videoRepository.findById((videoId))
                .orElseThrow(() -> new IllegalStateException("Video not found: " + videoId));

        if (video.getStatus() == UploadProcess.DONE) {
            return;
        }

        video.setStatus(UploadProcess.PROCESSING);
        videoRepository.save(video);

        var tempDir = Path.of(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());

        try {
            Files.createDirectories(tempDir);

            var safeFileName = Path.of(video.getFileName()).getFileName().toString();
            var rawVideoPath = tempDir.resolve(safeFileName);

            s3Client.downloadFile(video.getFolder() + "/" + video.getFileName(), rawVideoPath);

            var duration = ffMpegService.getVideoDuration(rawVideoPath);
            video.setDuration(duration);

            var outputDir = tempDir.resolve("hls_output");
            Files.createDirectories(outputDir);

            log.info("Start FFmpeg videoId={}", videoId);

            ffMpegService.runFfmpegMultiBitrate(rawVideoPath, outputDir, duration, videoId);

            var hlsFolder = "hls-videos/" + video.getUserId() + "/" + video.getId();

            var master = outputDir.resolve("master.m3u8");
            ffMpegService.uploadFolder(outputDir, hlsFolder);

            video.setStatus(UploadProcess.DONE);
            video.setHlsUrl(hlsFolder + "/" + master.getFileName());

            // thumbnail
            var thumbnailPath = ffMpegService.generateThumbnail(rawVideoPath, tempDir);

            var thumbnailFolder = "thumbnails/" + video.getUserId();
            var thumbnailName = video.getId() + ".jpg";

            try (var is = Files.newInputStream(thumbnailPath)) {
                s3Client.uploadFile(thumbnailFolder, thumbnailName, is, "image/jpeg", Files.size(thumbnailPath));
            }

            video.setThumbnailUrl(thumbnailFolder + "/" + thumbnailName);

            videoRepository.save(video);

            log.info("Finished processing videoId={}", videoId);

        } catch (Exception e) {
            video.setStatus(UploadProcess.FAILED);
            videoRepository.save(video);

            log.error("Processing failed videoId={}", videoId, e);
            throw new RuntimeException(e);

        } finally {
            FileSystemUtils.deleteRecursively(tempDir.toFile());
        }
    }

    public MultipartInitResponse initMultipartUpload(
            final String userId,
            final String fileName,
            final String contentType) {

        var safeFileName = FileUtils.generateSafeFileName(fileName);

        var key = "raw-videos/" + userId + "/" + safeFileName;

        var uploadId = s3Client.createMultipartUpload(key, contentType);

        return MultipartInitResponse.builder()
                .key(key)
                .uploadId(uploadId)
                .build();
    }

    public MultipartUploadUrlResponse getUploadPartUrl(
            final String key,
            final String uploadId,
            final int partNumber) {

        var url = s3Client.generatePresignedUploadPartUrl(key, uploadId, partNumber);

        return MultipartUploadUrlResponse.builder()
                .url(url)
                .build();
    }

    @Transactional
    public VideoUploadResponse completeMultipartUpload(
            final String userId,
            final CompleteMultipartRequest request) {

        var parts = request.getParts().stream()
                .map(p -> CompletedPart.builder()
                        .partNumber(p.getPartNumber())
                        .eTag(p.getEtag())
                        .build())
                .toList();

        s3Client.completeMultipartUpload(
                request.getKey(),
                request.getUploadId(),
                parts
        );

        var key = request.getKey();
        var folder = key.substring(0, key.lastIndexOf("/"));
        var fileName = Path.of(key).getFileName().toString();

        var document = Video.builder()
                .userId(userId)
                .title(StringUtils.defaultString(request.getTitle()))
                .description(StringUtils.defaultString(request.getDescription()))
                .folder(folder)
                .fileName(fileName)
                .contentType(request.getContentType())
                .fileSize(request.getSize())
                .status(UploadProcess.CREATED)
                .build();

        var saved = videoRepository.save(document);

        publisher.publishEvent(OnUploadVideoEvent.builder()
                .videoId(saved.getId())
                .build());

        return VideoUploadResponse.builder()
                .videoId(saved.getId())
                .status(saved.getStatus())
                .build();
    }
}
