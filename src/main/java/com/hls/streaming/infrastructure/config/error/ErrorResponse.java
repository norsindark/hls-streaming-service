package com.hls.streaming.infrastructure.config.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private long errorCode;
    private String message;
    private String path;
    private Instant timestamp;
    private String error;
    private Object optionalData;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ErrorField> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorField {
        private String message;
        private String field;
        private Object rejectedValue;
    }
}
