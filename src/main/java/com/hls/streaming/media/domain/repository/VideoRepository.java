package com.hls.streaming.media.domain.repository;

import com.hls.streaming.media.domain.document.Video;
import com.hls.streaming.media.domain.enums.VideoStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends MongoRepository<Video, String> {

    @Query(value = "{ 'user_id': ?0 }", sort = "{ 'created_at': -1 }")
    List<Video> findVideosByUserId(final String userId, Pageable pageable);

    @Query(value = "{ 'user_id': ?0 }", count = true)
    long countVideosByUserId(final String userId);

    @Query("{ '_id': ?0, 'status': ?1 }")
    Optional<Video> findVideoByIdAndStatus(String id, VideoStatus status);
}
