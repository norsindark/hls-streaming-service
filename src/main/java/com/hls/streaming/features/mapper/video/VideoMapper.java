package com.hls.streaming.features.mapper.video;

import com.hls.streaming.documents.media.Video;
import com.hls.streaming.dtos.media.VideoResponse;
import com.hls.streaming.features.mapper.CentralConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralConfig.class)
public interface VideoMapper {

    @Mapping(target = "videoId", source = "id")
    @Mapping(target = "status", expression = "java(doc.getStatus().name())")
    @Mapping(target = "duration", expression = "java(doc.getDuration() != null ? (int) Math.floor(doc.getDuration()) : 0)")
    VideoResponse toResponse(Video doc);
}
