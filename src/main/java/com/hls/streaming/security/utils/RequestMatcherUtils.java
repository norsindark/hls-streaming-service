package com.hls.streaming.security.utils;

import com.hls.streaming.security.models.HttpRequestMapping;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class RequestMatcherUtils {

    public static RequestMatcher createRequestMatcherWithOrPatternByPaths(final Set<String> paths) {
        final List<RequestMatcher> matchers =
                CollectionUtils.emptyIfNull(paths)
                        .stream()
                        .map(AntPathRequestMatcher::new)
                        .collect(Collectors.toList());
        return new OrRequestMatcher(matchers);
    }

    public static RequestMatcher createPermitAll() {
        return request -> false;
    }

    public static RequestMatcher createRequestMatcher(final HttpRequestMapping request) {
        return new AntPathRequestMatcher(request.getPath(), request.getMethod().toString());
    }

    public static RequestMatcher createRequestMatcherWithOrPattern(final List<HttpRequestMapping> requests) {
        return new OrRequestMatcher(requests.stream()
                .map(req -> new AntPathRequestMatcher(req.getPath(), req.getMethod().toString())).collect(Collectors.toList()));
    }
}
