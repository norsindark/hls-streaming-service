package com.hls.streaming.media.domain.repository;

import com.hls.streaming.media.domain.document.VideoUploadProcess;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoUploadProcessRepository extends MongoRepository<VideoUploadProcess, String> {
}
