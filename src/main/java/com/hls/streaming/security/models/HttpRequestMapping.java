package com.hls.streaming.security.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RequestMethod;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpRequestMapping {

    private RequestMethod method;
    private String path;
}
