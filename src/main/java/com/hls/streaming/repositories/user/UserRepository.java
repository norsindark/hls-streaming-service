package com.hls.streaming.repositories.user;

import com.hls.streaming.documents.user.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, ObjectId> {

    Optional<User> findByUsername(final String username);

    Optional<User> findByEmail(final String email);

    @Query("{ 'email' : ?0 }")
    Optional<User> findCustomUserByEmail(final String email);

    boolean existsByUsername(final String username);

    boolean existsByEmail(final String email);
}
