package com.hls.streaming.config.properties;

import com.hls.streaming.security.models.TokenKeyPair;
import com.hls.streaming.security.models.TokenType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@Data
@ConfigurationProperties("io.hls.token")
public class TokenConfigProperties {

    private String issuer;
    private String tokenKeypairId;
    private List<TokenConfig> tokenConfig;

    private TokenKeyPair keyPair;

    @Getter
    @Setter
    public static class TokenConfig {
        private TokenType tokenType;
        private Duration livedTime;
    }
}
