package com.hls.streaming.services.media.processing.internal;

import com.hls.streaming.dtos.media.VideoProgressEvent;
import com.hls.streaming.enums.UploadProcess;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProgressPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(final String videoId, UploadProcess process, final int percent) {

        messagingTemplate.convertAndSend(
                "/topic/video-progress/" + videoId,
                VideoProgressEvent.builder()
                        .videoId(videoId)
                        .status(process)
                        .percent(percent)
                        .build()
        );
    }
}
