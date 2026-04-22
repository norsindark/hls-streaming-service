package com.hls.streaming.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "io.hls.ffmpeg.executor")
public class FfmpegExecutorProperties {
    private int coreSize;
    private int maxSize;
    private int queueCapacity;
    private int keepAliveSeconds;
}
