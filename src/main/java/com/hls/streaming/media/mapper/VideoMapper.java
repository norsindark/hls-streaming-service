package com.hls.streaming.media.mapper;

import com.hls.streaming.infrastructure.config.mapper.CentralConfig;
import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.media.dto.VideoResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Objects;

@Mapper(config = CentralConfig.class)
public interface VideoMapper {

    @Mapping(target = "videoId", source = "id")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "duration", ignore = true)
    VideoResponse toResponse(Video doc);

    @AfterMapping
    default void afterToResponse(Video doc, @MappingTarget VideoResponse response) {
        if (Objects.nonNull(doc) && Objects.nonNull(response)) {
            var status = doc.getStatus();
            if (Objects.nonNull(status)) {
                response.setStatus(status.name());
            }

            var duration = doc.getDuration();
            if (Objects.nonNull(duration)) {
                var roundedDuration = (int) Math.floor(duration);
                response.setDuration(roundedDuration);
            } else {
                response.setDuration(0);
            }
        }
    }
}
