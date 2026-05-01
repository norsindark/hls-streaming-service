package com.hls.streaming.media.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbortUploadVideoRequest {

    @JsonIgnore
    private String userId;

    private String key;
    private String uploadId;
}
