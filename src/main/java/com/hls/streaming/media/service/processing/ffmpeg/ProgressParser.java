package com.hls.streaming.media.service.processing.ffmpeg;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProgressParser {

    public static int parse(final String line, final double duration) {

        try {
            var idx = line.indexOf("time=");
            if (idx == -1) {
                return 0;
            }

            var time = line.substring(idx + 5).split(" ")[0];

            if (time.contains("N/A")) {
                return 0;
            }

            var parts = time.split(":");

            var seconds = Integer.parseInt(parts[0]) * 3600 +
                            Integer.parseInt(parts[1]) * 60 +
                            Double.parseDouble(parts[2]);

            return (int) ((seconds / duration) * 100);

        } catch (Exception e) {
            log.warn("Parse failed line={}", line);
            return 0;
        }
    }
}
