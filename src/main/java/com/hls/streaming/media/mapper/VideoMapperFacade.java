package com.hls.streaming.media.mapper;

import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.media.dto.VideoResponse;
import com.hls.streaming.infrastructure.storage.S3Client;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VideoMapperFacade {

    private final VideoMapper mapper;
    private final S3Client s3Client;

    public VideoResponse toResponse(final Video doc) {
        var res = mapper.toResponse(doc);

        res.setHlsUrl(s3Client.generatePresignedUrl(doc.getHlsUrl(), HttpMethod.GET));
        res.setThumbnailUrl(s3Client.generatePresignedUrl(doc.getThumbnailUrl(), HttpMethod.GET));

        return res;
    }
}
