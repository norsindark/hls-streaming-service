package com.hls.streaming.security.models;

import com.hls.streaming.security.permissions.authorizer.RequestAuthorizer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpRequestSecurity {

    private HttpRequestMapping httpRequest;
    private RequestMatcher requestMatcher;
    private List<RequestAuthorizer> authorizers;
    private String permissionsExpression;
}
