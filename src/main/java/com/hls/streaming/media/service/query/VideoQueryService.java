package com.hls.streaming.media.service.query;

import com.hls.streaming.common.dtos.PageResponse;
import com.hls.streaming.common.exception.BadRequestException;
import com.hls.streaming.infrastructure.config.error.ErrorCodeConfig;
import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.media.domain.enums.VideoStatus;
import com.hls.streaming.media.domain.repository.VideoRepository;
import com.hls.streaming.media.dto.VideoResponse;
import com.hls.streaming.media.mapper.VideoMapperFacade;
import com.hls.streaming.media.utils.MediaUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

import static com.hls.streaming.common.constant.ErrorConfigConstants.INVALID_CURSOR;

@Service
@RequiredArgsConstructor
public class VideoQueryService {

    private final VideoRepository videoRepository;
    private final VideoMapperFacade videoMapperFacade;
    private final ErrorCodeConfig errorCodeConfig;

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

    public PageResponse<VideoResponse> getHomeFeed(final String cursor, final Pageable pageable) {

        var videos = resolveVideos(cursor, pageable);

        var content = videos.stream()
                .map(videoMapperFacade::toResponse)
                .toList();

        var nextCursor = buildNextCursor(videos);

        return PageResponse.<VideoResponse> builder()
                .content(content)
                .totalElements(-1)
                .totalPages(-1)
                .nextCursor(nextCursor)
                .build();
    }

    private List<Video> resolveVideos(final String cursor, final Pageable pageable) {

        if (!StringUtils.hasText(cursor)) {
            return videoRepository.findTopByStatusOrderByCreatedAtDescIdDesc(
                    VideoStatus.DONE,
                    pageable
            );
        }

        var cursorData = parseCursor(cursor);

        return videoRepository.findNextFeed(
                VideoStatus.DONE,
                cursorData.createdAt(),
                cursorData.id(),
                pageable
        );
    }

    private CursorData parseCursor(final String cursor) {

        try {
            var decoded = MediaUtils.decodeCursor(cursor);

            var separatorIndex = decoded.indexOf('_');
            if (separatorIndex < 0) {
                throw new BadRequestException(errorCodeConfig.getMessage(INVALID_CURSOR));
            }

            var epochMilli = Long.parseLong(decoded.substring(0, separatorIndex));
            var id = decoded.substring(separatorIndex + 1);

            return new CursorData(Instant.ofEpochMilli(epochMilli), id);

        } catch (Exception ex) {
            throw new BadRequestException(errorCodeConfig.getMessage(INVALID_CURSOR));
        }
    }

    private String buildNextCursor(final List<Video> videos) {

        if (CollectionUtils.isEmpty(videos)) {
            return null;
        }

        var last = videos.getLast();

        return MediaUtils.encodeCursor(
                last.getCreatedAt().toEpochMilli(),
                last.getId()
        );
    }

    private record CursorData(Instant createdAt, String id) {
    }
}
