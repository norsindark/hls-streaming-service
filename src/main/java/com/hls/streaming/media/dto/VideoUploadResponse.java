package com.hls.streaming.media.dto;

import com.hls.streaming.media.domain.enums.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadResponse {
    private String videoId;
    private VideoStatus status;
}
