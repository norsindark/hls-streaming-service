package com.hls.streaming.security.authentication.service;

import com.hls.streaming.security.authentication.component.TokenSupporter;
import com.hls.streaming.security.authentication.model.TokenClaim;
import com.hls.streaming.security.authentication.model.TokenType;
import com.hls.streaming.security.authentication.model.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringTokenService {

    private final TokenSupporter tokenSupporter;
    private final MonitoringTokenFileWriter monitoringTokenFileWriter;

    @EventListener(ApplicationReadyEvent.class)
    public void writeMonitoringTokenOnStartup() {
        try {
            rotateMonitoringToken();
        } catch (IOException ex) {
            log.warn("Unable to write monitoring token file. Prometheus scraping may fail.", ex);
        } catch (RuntimeException ex) {
            log.error("Unable to generate monitoring token. Prometheus scraping may fail.", ex);
        }
    }

    public void rotateMonitoringToken() throws IOException {
        final var token = generateMonitoringToken();
        monitoringTokenFileWriter.write(token);
    }

    public String generateMonitoringToken() {
        final var tokenClaim = TokenClaim.builder()
                .userId(TokenSupporter.SYSTEM_ID)
                .privileges(Set.of(UserRole.MONITORING))
                .type(TokenType.ACCESS_TOKEN)
                .build();

        return tokenSupporter.generateTokenInternal(tokenClaim);
    }
}
