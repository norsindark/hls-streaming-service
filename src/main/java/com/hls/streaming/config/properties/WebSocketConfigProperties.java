package com.hls.streaming.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties("io.hls.websocket")
public class WebSocketConfigProperties {

    private List<String> endpoints;
}
