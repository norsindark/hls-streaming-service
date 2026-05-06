package com.hls.streaming.media.domain.repository;

import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.media.domain.enums.VideoStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends MongoRepository<Video, String> {

    @Query(value = "{ 'user_id': ?0 }", sort = "{ 'created_at': -1 }")
    List<Video> findVideosByUserId(final String userId, Pageable pageable);

    @Query(value = "{ 'user_id': ?0 }", count = true)
    long countVideosByUserId(final String userId);

    @Query("{ '_id': ?0, 'status': ?1 }")
    Optional<Video> findVideoByIdAndStatus(final String id, final VideoStatus status);

    @Query("{ '_id': ?0, 'user_id': ?1, 'status': ?2 }")
    void updateStatusByVideoIdAndUserId(final String videoId, final String userId, final VideoStatus videoStatus);

    @Query("{ 'object_key': ?0, 'user_id': ?1 }")
    Optional<Video> findVideoByObjectKeyAndUserId(final String key, final String userId);

    @Query(value = "{ 'status': ?0 }", sort = "{ 'created_at': -1, '_id': -1 }")
    List<Video> findTopByStatusOrderByCreatedAtDescIdDesc(
            final VideoStatus status,
            final Pageable pageable);

    @Query(value = "{" +
            " 'status': ?0, " +
            " $or: [" +
            "   { 'created_at': { $lt: ?1 } }, " +
            "   { 'created_at': ?1, '_id': { $lt: ?2 } }" +
            " ]" +
            "}", sort = "{ 'created_at': -1, '_id': -1 }")
    List<Video> findNextFeed(
            final VideoStatus status,
            final Instant createdAt,
            final String id,
            final Pageable pageable
    );
}
