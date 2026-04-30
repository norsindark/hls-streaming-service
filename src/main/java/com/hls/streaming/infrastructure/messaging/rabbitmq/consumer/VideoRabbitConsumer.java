package com.hls.streaming.infrastructure.messaging.rabbitmq.consumer;

import com.hls.streaming.media.event.OnUploadVideoEvent;
import com.hls.streaming.media.service.processing.VideoProcessingOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "io.hls.app.listener.enabled", havingValue = "true")
public class VideoRabbitConsumer {

    private final VideoProcessingOrchestrator videoProcessingOrchestrator;

    @RabbitListener(queues = "${io.hls.rabbitmq.ffmpeg.queue.name}",
            concurrency = "${io.hls.rabbitmq.ffmpeg.queue.concurrency:1-2}")
    public void handleProcessVideoMessage(final OnUploadVideoEvent event) {
        if (Objects.nonNull(event)) {

            var videoId = event.getVideoId();

            if (StringUtils.isNotBlank(event.getVideoId())) {
                try {
                    videoProcessingOrchestrator.process(videoId);
                    log.info("Finished processing videoId = {}", videoId);
                } catch (Exception e) {
                    log.error("FFMpeg processing failed for videoId = {}", videoId, e);
                    throw e;
                }
            }
        }
    }
}
