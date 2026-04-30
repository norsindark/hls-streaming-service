package com.hls.streaming.security.authorization.extractor;

import com.hls.streaming.security.authentication.model.HttpRequestMapping;
import lombok.experimental.UtilityClass;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

@UtilityClass
public class HttpMethodExtractor {

    public static String getPreAuthorizeExpression(final Method method) {
        return Optional.ofNullable(AnnotationUtils.findAnnotation(method, PreAuthorize.class))
                .map(PreAuthorize::value)
                .orElse(null);
    }

    public static Optional<HttpRequestMapping> getHttpMappingAnnotation(final Method method) {
        final var builder = HttpRequestMapping.builder();
        final var requestMappingAnnotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);

        if (Objects.isNull(requestMappingAnnotation)) {
            return Optional.empty();
        }

        switch (requestMappingAnnotation.method()[0]) {
        case GET:
            final var getMappingAnnotation = Optional.ofNullable(AnnotationUtils.findAnnotation(method, GetMapping.class))
                    .orElseThrow(() -> new IllegalStateException("Not Found Get Mapping Annotation"));
            builder.path(getMappingAnnotation.value()[0]);
            break;
        case POST:
            final var postMappingAnnotation = Optional.ofNullable(AnnotationUtils.findAnnotation(method, PostMapping.class))
                    .orElseThrow(() -> new IllegalStateException("Not Found Post Mapping Annotation"));
            builder.path(postMappingAnnotation.value()[0]);
            break;
        case PUT:
            final var putMappingAnnotation = Optional.ofNullable(AnnotationUtils.findAnnotation(method, PutMapping.class))
                    .orElseThrow(() -> new IllegalStateException("Not Found Put Mapping Annotation"));
            builder.path(putMappingAnnotation.value()[0]);
            break;
        case DELETE:
            final var deleteMappingAnnotation = Optional.ofNullable(AnnotationUtils.findAnnotation(method, DeleteMapping.class))
                    .orElseThrow(() -> new IllegalStateException("Not Found Delete Mapping Annotation"));
            builder.path(deleteMappingAnnotation.value()[0]);
            break;
        case PATCH:
            final var patchMappingAnnotation = Optional.ofNullable(AnnotationUtils.findAnnotation(method, PatchMapping.class))
                    .orElseThrow(() -> new IllegalStateException("Not Found Patch Mapping Annotation"));
            builder.path(patchMappingAnnotation.value()[0]);
            break;
        default:
            throw new IllegalStateException(String.format("Un-support method: %s", method.getName()));
        }
        return Optional.of(builder.method(requestMappingAnnotation.method()[0]).build());
    }

    public static String stripAuthorizedExpression(final String expression) {
        return expression.substring(expression.indexOf("(") + 1, expression.indexOf(")"));
    }
}
