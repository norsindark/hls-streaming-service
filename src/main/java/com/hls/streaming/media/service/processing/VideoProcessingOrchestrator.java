package com.hls.streaming.media.service.processing;

import com.hls.streaming.media.domain.enums.VideoStatus;
import com.hls.streaming.media.domain.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoProcessingOrchestrator {

    private final VideoRepository videoRepository;
    private final VideoDownloader videoDownloader;
    private final VideoProbeService probeService;
    private final VideoTranscoder transcoder;
    private final HlsUploader hlsUploader;
    private final ThumbnailGenerator thumbnailGenerator;

    public void process(String videoId) {

        var video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalStateException("Video not found"));

        if (video.getStatus() == VideoStatus.DONE) {
            return;
        }

        video.setStatus(VideoStatus.PROCESSING);
        videoRepository.save(video);

        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());

        try {
            Files.createDirectories(tempDir);

            var rawPath = videoDownloader.download(video, tempDir);

            var duration = probeService.getDuration(rawPath);
            video.setDuration(duration);

            var outputDir = Files.createDirectories(tempDir.resolve("hls"));

            transcoder.transcode(rawPath, outputDir, duration, video.getId());

            var hlsUrl = hlsUploader.upload(video, outputDir);
            video.setHlsUrl(hlsUrl);

            var thumbnailUrl = thumbnailGenerator.generateAndUpload(video, rawPath, tempDir);
            video.setThumbnailUrl(thumbnailUrl);

            video.setStatus(VideoStatus.DONE);
            videoRepository.save(video);

            log.info("Finished processing videoId={}", videoId);

        } catch (Exception e) {
            video.setStatus(VideoStatus.FAILED);
            videoRepository.save(video);

            log.error("Processing failed videoId={}", videoId, e);
            throw new RuntimeException(e);

        } finally {
            FileSystemUtils.deleteRecursively(tempDir.toFile());
        }
    }
}
