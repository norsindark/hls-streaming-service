package com.hls.streaming.media.service.processing;

import com.hls.streaming.media.domain.enums.UploadProcess;
import com.hls.streaming.media.service.processing.ffmpeg.FfmpegCommandFactory;
import com.hls.streaming.media.service.processing.ffmpeg.FfmpegProcessRegistry;
import com.hls.streaming.media.service.processing.ffmpeg.ProgressParser;
import com.hls.streaming.media.service.processing.ffmpeg.ProgressPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

@Slf4j
@Service
public class VideoTranscoder {

    private final ProgressPublisher progressPublisher;
    private final FfmpegProcessRegistry processRegistry;
    private final MeterRegistry registry;
    private final Timer segmentGenerationSuccessTimer;
    private final Timer segmentGenerationFailureTimer;

    public VideoTranscoder(
            final ProgressPublisher progressPublisher,
            final FfmpegProcessRegistry processRegistry,
            final MeterRegistry registry) {

        this.progressPublisher = progressPublisher;
        this.processRegistry = processRegistry;
        this.registry = registry;
        this.segmentGenerationSuccessTimer = segmentGenerationTimer("success");
        this.segmentGenerationFailureTimer = segmentGenerationTimer("failure");
    }

    public void transcode(final Path input,
            final Path outputDir,
            final double duration,
            final String videoId,
            final String key) throws Exception {

        final Timer.Sample sample = Timer.start(registry);
        boolean success = false;

        try {
            final Process process = FfmpegCommandFactory.create(input, outputDir);

            processRegistry.register(key, process);

            int lastPercent = -1;

            try (final BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {

                    if (processRegistry.isCancelled(key)) {
                        log.warn("Transcoding cancelled: {}", videoId);
                        throw new RuntimeException("Video aborted");
                    }

                    if (line.contains("time=")) {
                        final int percent = ProgressParser.parse(line, duration);

                        if (percent != lastPercent) {
                            lastPercent = percent;
                            log.info("Transcoding video {}, {}%", videoId, percent);
                            progressPublisher.publish(videoId, UploadProcess.PROCESSING, percent);
                        }
                    }
                }
            } finally {
                processRegistry.remove(key);
            }

            if (process.waitFor() != 0) {
                throw new RuntimeException("FFmpeg failed");
            }

            progressPublisher.publish(videoId, UploadProcess.DONE, 100);
            success = true;
        } finally {
            sample.stop(success ? segmentGenerationSuccessTimer : segmentGenerationFailureTimer);
        }
    }

    private Timer segmentGenerationTimer(final String outcome) {
        return Timer.builder("hls.segment.generation")
                .description("Latency of HLS segment and playlist generation by ffmpeg")
                .tag("outcome", outcome)
                .publishPercentileHistogram()
                .register(registry);
    }
}
