package com.hls.streaming.security.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpRequestSecurity {

    private HttpRequestMapping httpRequest;
    private RequestMatcher requestMatcher;
    private String permissionsExpression;
}
