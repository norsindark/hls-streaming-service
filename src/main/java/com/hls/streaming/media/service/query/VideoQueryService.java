package com.hls.streaming.media.service.query;

import com.hls.streaming.common.dtos.PageResponse;
import com.hls.streaming.media.dto.VideoResponse;
import com.hls.streaming.media.domain.enums.VideoStatus;
import com.hls.streaming.media.mapper.VideoMapperFacade;
import com.hls.streaming.media.domain.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoQueryService {

    private final VideoRepository videoRepository;
    private final VideoMapperFacade videoMapperFacade;

    public VideoResponse getVideoById(final String videoId) {

        var video = videoRepository.findVideoByIdAndStatus(videoId, VideoStatus.DONE)
                .orElseThrow(() -> new IllegalStateException("Video not found or not ready"));
        return videoMapperFacade.toResponse(video);
    }

    public PageResponse<VideoResponse> getVideosByUser(String userId, Pageable pageable) {

        var videos = videoRepository.findVideosByUserId(userId, pageable);
        var total = videoRepository.countVideosByUserId(userId);

        var content = videos.stream()
                .map(videoMapperFacade::toResponse)
                .toList();

        var totalPages = (long) Math.ceil((double) total / pageable.getPageSize());

        return PageResponse.<VideoResponse> builder()
                .totalElements(total)
                .totalPages(totalPages)
                .content(content)
                .build();
    }
}
