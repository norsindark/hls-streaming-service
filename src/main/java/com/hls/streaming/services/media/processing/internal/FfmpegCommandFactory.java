package com.hls.streaming.services.media.processing.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class FfmpegCommandFactory {

    public static Process create(Path input, Path outputDir) throws IOException {

        var command = List.of(
                "ffmpeg",
                "-i", input.toString(),

                "-filter_complex",
                "[0:v]split=3[v1][v2][v3];" +
                        "[v1]scale=640:360[v1out];" +
                        "[v2]scale=842:480[v2out];" +
                        "[v3]scale=1280:720[v3out]",

                "-map", "[v1out]", "-map", "a:0",
                "-map", "[v2out]", "-map", "a:0",
                "-map", "[v3out]", "-map", "a:0",

                "-c:v", "libx264",
                "-c:a", "aac",

                "-f", "hls",
                "-hls_time", "6",
                "-hls_playlist_type", "vod",

                "-var_stream_map", "v:0,a:0 v:1,a:1 v:2,a:2",
                "-master_pl_name", "master.m3u8",

                "-hls_segment_filename",
                outputDir.resolve("v%v_segment_%03d.ts").toString(),

                outputDir.resolve("v%v.m3u8").toString()
        );

        var pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        return pb.start();
    }
}
