package com.hls.streaming.media.mapper;

import com.hls.streaming.infrastructure.config.mapper.CentralConfig;
import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.media.dto.VideoResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = CentralConfig.class)
public interface VideoMapper {

    @Mapping(target = "videoId", source = "id")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "duration", ignore = true)
    VideoResponse toResponse(Video doc);

    @AfterMapping
    default void afterToResponse(
            final Video doc,
            @MappingTarget final VideoResponse.VideoResponseBuilder response) {
        if (doc == null) {
            return;
        }

        var status = doc.getStatus();
        if (status != null) {
            response.status(status.name());
        }

        var duration = doc.getDuration();
        response.duration(duration != null ? (int) Math.floor(duration) : 0);
    }
}
