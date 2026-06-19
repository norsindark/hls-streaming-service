package com.hls.streaming.security.authentication.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

@Slf4j
@Service
public class MonitoringTokenFileWriter {

    private final Path tokenFile;

    public MonitoringTokenFileWriter(@Value("${io.hls.monitoring.token-file:prometheus/token}") final Path tokenFile) {
        this.tokenFile = tokenFile;
    }

    public void write(final String token) throws IOException {
        final var target = tokenFile.toAbsolutePath().normalize();
        final var parent = target.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        assert parent != null;
        final var temporaryFile = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        try {
            Files.writeString(
                    temporaryFile,
                    token,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            moveTokenFile(temporaryFile, target);
            log.info("Monitoring token file updated at {}", target);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private void moveTokenFile(final Path temporaryFile, final Path target) throws IOException {
        try {
            Files.move(
                    temporaryFile,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
