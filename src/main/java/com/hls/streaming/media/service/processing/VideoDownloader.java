package com.hls.streaming.media.service.processing;

import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.infrastructure.storage.S3Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class VideoDownloader {

    private final S3Client s3Client;

    public Path download(Video video, Path dir) throws IOException {

        var safeName = Path.of(video.getFileName()).getFileName().toString();
        var path = dir.resolve(safeName);

        s3Client.downloadFile(video.getFolder() + "/" + video.getFileName(), path);

        return path;
    }
}
