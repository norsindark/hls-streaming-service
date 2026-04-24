package com.hls.streaming.config.error;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "io.hls")
@Component
@Data
public class ExternalErrorCodeProperties {

    private List<ErrorCodeMessage> errorCodes;
}
