package com.hls.streaming.utils;

import com.hls.streaming.config.properties.StorageConfig;
import com.hls.streaming.features.mapper.qualifier.PresignedUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MediaUrlUtils {

    private final StorageConfig storageConfig;

    @PresignedUrl
    public String toUrl(final String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return storageConfig.getFullEndpointUrl()
                + "/"
                + storageConfig.getBucketName()
                + "/"
                + key;
    }
}
