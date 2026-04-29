package com.hls.streaming.features.mapper.video;

import com.hls.streaming.documents.media.Video;
import com.hls.streaming.dtos.media.VideoResponse;
import com.hls.streaming.storage.S3Client;
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
