package com.hls.streaming.repositories.media;

import com.hls.streaming.documents.media.VideoUploadProcess;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoUploadProcessRepository extends MongoRepository<VideoUploadProcess, String> {
}
