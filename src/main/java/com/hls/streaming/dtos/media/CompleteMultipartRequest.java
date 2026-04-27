package com.hls.streaming.dtos.media;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteMultipartRequest {
    private String key;
    private String uploadId;
    private String fileName;
    private String title;
    private String description;
    private long size;
    private String contentType;
    private List<Part> parts;

    @Getter
    @Setter
    public static class Part {
        private int partNumber;
        private String etag;
    }
}
