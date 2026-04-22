package com.hls.streaming.services.media;

import com.hls.streaming.storage.S3Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FFMpegService {

    private final S3Client s3Client;

    public void runFfmpegMultiBitrate(Path input, Path outputDir, double duration, String videoId)
            throws IOException, InterruptedException {

        Path master = outputDir.resolve("master.m3u8");

        Process process = getProcess(input, outputDir);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {

                if (line.contains("time=")) {
                    double current = parseTime(line);
                    int percent = (int) ((current / duration) * 100);

                    log.info("[FFMPEG] videoId={} progress={}%", videoId, percent);
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed, exitCode=" + exitCode);
        }
    }

    public static Process getProcess(Path input, Path outputDir) throws IOException {
        var pb = new ProcessBuilder(
                "ffmpeg",
                "-i", input.toString(),

                "-filter_complex",
                "[0:v]split=3[v1][v2][v3];" +
                        "[v1]scale=w=640:h=360[v1out];" +
                        "[v2]scale=w=842:h=480[v2out];" +
                        "[v3]scale=w=1280:h=720[v3out]",

                "-map", "[v1out]", "-map", "a:0",
                "-map", "[v2out]", "-map", "a:0",
                "-map", "[v3out]", "-map", "a:0",

                "-b:v:0", "800k",
                "-b:v:1", "1400k",
                "-b:v:2", "2800k",

                "-c:v", "libx264",
                "-c:a", "aac",

                "-f", "hls",
                "-hls_time", "6",
                "-hls_playlist_type", "vod",

                "-var_stream_map", "v:0,a:0 v:1,a:1 v:2,a:2",
                "-master_pl_name", "master.m3u8",

                "-hls_segment_filename",
                outputDir.resolve("v%v_segment_%03d.ts").toString(),

                outputDir.resolve("v%v.m3u8").toString());

        pb.redirectErrorStream(true);
        return pb.start();
    }

    public double getVideoDuration(Path file) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                file.toString());

        pb.redirectErrorStream(true);
        Process p = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()))) {

            String line = reader.readLine();
            p.waitFor();
            return Double.parseDouble(line);
        }
    }

    private double parseTime(String line) {
        int idx = line.indexOf("time=");
        if (idx == -1)
            return 0;

        String time = line.substring(idx + 5, idx + 16);

        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 3600 +
                Integer.parseInt(parts[1]) * 60 +
                Double.parseDouble(parts[2]);
    }

    public void uploadFolder(Path folder, String s3Folder) throws IOException {
        try (Stream<Path> stream = Files.walk(folder)) {
            stream.filter(Files::isRegularFile).forEach(file -> {

                String name = file.getFileName().toString();
                String contentType = name.endsWith(".m3u8")
                        ? "application/x-mpegURL"
                        : "video/MP2T";

                try (InputStream is = Files.newInputStream(file)) {
                    s3Client.uploadFile(s3Folder, name, is, contentType, Files.size(file));
                } catch (Exception e) {
                    throw new RuntimeException("Upload failed: " + name, e);
                }
            });
        }
    }
}
