package com.hls.streaming.services.media;

import com.hls.streaming.documents.media.VideoDocument;
import com.hls.streaming.dtos.events.OnUploadVideoEvent;
import com.hls.streaming.dtos.media.VideoUploadResponse;
import com.hls.streaming.enums.UploadProcess;
import com.hls.streaming.repositories.media.VideoRepository;
import com.hls.streaming.storage.S3Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

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

        var folder = "raw-videos/" + userId;
        var contentType = StringUtils.defaultIfBlank(file.getContentType(), "video/mp4");

        var document = VideoDocument.builder()
                .userId(userId)
                .title(StringUtils.defaultString(title))
                .description(StringUtils.defaultString(description))
                .folder(folder)
                .fileName(fileName)
                .contentType(contentType)
                .fileSize(file.getSize())
                .status(UploadProcess.CREATED)
                .build();

        var saved = videoRepository.save(document);

        try {
            s3Client.uploadFile(folder, fileName, file.getInputStream(), contentType, file.getSize());
        } catch (Exception e) {
            saved.setStatus(UploadProcess.FAILED);
            videoRepository.save(saved);
            throw new RuntimeException("Upload failed", e);
        }

        publisher.publishEvent(OnUploadVideoEvent.builder()
                .videoId(saved.getId())
                .build());

        return VideoUploadResponse.builder()
                .videoId(saved.getId())
                .status(saved.getStatus())
                .build();
    }

    public void processHlsConversion(final String videoId) {

        var video = videoRepository.findById(new ObjectId(videoId))
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
}
