package com.hls.streaming.dtos.media;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VideoResponse {

    private String videoId;
    private String title;
    private String description;
    private String hlsUrl;
    private String thumbnailUrl;
    private String status;
    private Long fileSize;
    private Integer duration;
    private Instant createdAt;
}
