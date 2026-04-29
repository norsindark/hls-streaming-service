package com.hls.streaming.services.media.processing;

import com.hls.streaming.enums.UploadProcess;
import com.hls.streaming.services.media.processing.internal.FfmpegCommandFactory;
import com.hls.streaming.services.media.processing.internal.ProgressParser;
import com.hls.streaming.services.media.processing.internal.ProgressPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoTranscoder {

    private final ProgressPublisher progressPublisher;

    public void transcode(Path input, Path outputDir, double duration, String videoId)
            throws Exception {

        var process = FfmpegCommandFactory.create(input, outputDir);

        int lastPercent = -1;

        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {

                if (line.contains("time=")) {

                    var percent = ProgressParser.parse(line, duration);

                    if (percent != lastPercent) {
                        lastPercent = percent;
                        log.info("Transcoding of video {}, process = {}%", videoId, percent);
                        progressPublisher.publish(videoId, UploadProcess.PROCESSING, percent);
                    }
                }
            }
        }

        if (process.waitFor() != 0) {
            throw new RuntimeException("FFmpeg failed");
        }

        progressPublisher.publish(videoId,UploadProcess.DONE, 100);
    }
}
