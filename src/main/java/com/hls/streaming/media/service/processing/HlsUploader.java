package com.hls.streaming.media.service.processing;

import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.infrastructure.storage.S3Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class HlsUploader {

    private final S3Client s3Client;

    public String upload(Video video, Path folder) throws IOException {

        var s3Folder = "hls-videos/" + video.getUserId() + "/" + video.getId();

        try (var stream = Files.walk(folder)) {

            stream.filter(Files::isRegularFile).forEach(file -> {

                try (var is = Files.newInputStream(file)) {

                    var name = file.getFileName().toString();
                    var contentType = name.endsWith(".m3u8")
                            ? "application/x-mpegURL"
                            : "video/MP2T";

                    s3Client.uploadFile(
                            s3Folder,
                            name,
                            is,
                            contentType,
                            Files.size(file));

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return s3Folder + "/master.m3u8";
    }
}
