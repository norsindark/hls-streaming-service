package com.hls.streaming.features.mapper;

import com.hls.streaming.documents.media.Video;
import com.hls.streaming.dtos.media.VideoResponse;
import com.hls.streaming.features.mapper.qualifier.PresignedUrl;
import com.hls.streaming.utils.MediaUrlUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralConfig.class, uses = MediaUrlUtils.class)
public interface VideoMapper {

    @Mapping(target = "videoId", source = "id")
    @Mapping(target = "status", expression = "java(doc.getStatus().name())")
    @Mapping(target = "hlsUrl", source = "hlsUrl", qualifiedBy = PresignedUrl.class)
    @Mapping(target = "thumbnailUrl", source = "thumbnailUrl", qualifiedBy = PresignedUrl.class)
    VideoResponse toResponse(Video doc);
}
