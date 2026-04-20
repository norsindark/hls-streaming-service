package com.hls.streaming.repositories.user;

import com.hls.streaming.documents.user.UserDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<UserDocument, String> {

    Optional<UserDocument> findByUsername(final String username);

    Optional<UserDocument> findByEmail(final String email);

    @Query("{ 'email' : ?0 }")
    Optional<UserDocument> findCustomUserByEmail(final String email);

    boolean existsByUsername(final String username);
}
