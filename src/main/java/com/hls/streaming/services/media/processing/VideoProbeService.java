package com.hls.streaming.services.media.processing;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
public class VideoProbeService {

    public double getDuration(Path file) throws Exception {

        var command = List.of(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                file.toString()
        );

        var p = new ProcessBuilder(command).start();

        try (var reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {

            var line = reader.readLine();
            p.waitFor();

            if (StringUtils.isBlank(line) || line.contains("N/A")) {
                throw new IllegalStateException("Invalid duration");
            }

            return Double.parseDouble(line.trim());
        }
    }
}
