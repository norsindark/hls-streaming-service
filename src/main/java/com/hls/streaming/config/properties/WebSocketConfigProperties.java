package com.hls.streaming.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties("io.hls.websocket")
public class WebSocketConfigProperties {

    private List<String> endpoints;
}
