package com.hls.streaming.media.service.processing;

import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.infrastructure.storage.S3Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThumbnailGenerator {

    private final S3Client s3Client;

    public String generateAndUpload(Video video, Path input, Path tempDir)
            throws Exception {

        var thumb = tempDir.resolve("thumbnail.jpg");

        var command = List.of(
                "ffmpeg",
                "-i", input.toString(),
                "-ss", "00:00:01.000",
                "-vframes", "1",
                "-q:v", "2",
                thumb.toString()
        );

        var p = new ProcessBuilder(command).start();

        if (p.waitFor() != 0) {
            throw new RuntimeException("Thumbnail failed");
        }

        var folder = "thumbnails/" + video.getUserId();
        var name = video.getId() + ".jpg";

        try (var is = Files.newInputStream(thumb)) {

            s3Client.uploadFile(
                    folder,
                    name,
                    is,
                    "image/jpeg",
                    Files.size(thumb)
            );
        }

        return folder + "/" + name;
    }
}
