package com.hls.streaming.repositories.media;

import com.hls.streaming.documents.media.VideoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository extends MongoRepository<VideoDocument, String> {}
