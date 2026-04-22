package com.hls.streaming.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "io.hls.storage")
public class StorageConfig {

    private String accessKey;
    private String secretKey;
    private String endpointUrl;
    private String fullEndpointUrl;
    private String bucketName;

}
