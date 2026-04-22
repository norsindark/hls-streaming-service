package com.hls.streaming.dtos.media;

import com.hls.streaming.enums.UploadProcess;
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
    private UploadProcess status;
}
