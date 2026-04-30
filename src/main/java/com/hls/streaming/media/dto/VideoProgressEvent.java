package com.hls.streaming.media.dto;

import com.hls.streaming.media.domain.enums.UploadProcess;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoProgressEvent {
    private String videoId;
    private UploadProcess status;
    private int percent;
}
