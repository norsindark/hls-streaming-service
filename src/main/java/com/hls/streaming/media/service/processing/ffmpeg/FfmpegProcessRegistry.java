package com.hls.streaming.media.service.processing.ffmpeg;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class FfmpegProcessRegistry {

    private final ConcurrentHashMap<String, Process> processes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> abortedList = new ConcurrentHashMap<>();

    public void register(final String key, final Process process) {

        if (log.isDebugEnabled()) {
            log.debug("Registering process for key: {}, process: {}", key, process);
        }

        processes.put(key, process);
        abortedList.put(key, Boolean.FALSE);
    }

    public void remove(final String key) {

        if (log.isDebugEnabled()) {
            log.debug("Removing process for key: {}", key);
        }

        processes.remove(key);
        abortedList.remove(key);
    }

    public void cancel(final String key) {
        abortedList.put(key, Boolean.TRUE);

        final Process process = processes.get(key);
        if (Objects.nonNull(process) && process.isAlive()) {
            process.destroyForcibly();
        }

        if (log.isDebugEnabled()) {
            log.debug("Cancelled process for key: {}, process: {}", key, process);
        }
    }

    public boolean isCancelled(final String key) {
        return Boolean.TRUE.equals(abortedList.get(key));
    }
}
