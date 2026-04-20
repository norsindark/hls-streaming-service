package com.hls.streaming.config.migrations;

import com.hls.streaming.documents.user.UserDetail;
import com.hls.streaming.documents.user.UserDocument;
import com.hls.streaming.enums.UserStatusEnum;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;

@ChangeUnit(id = "init-default-admin-user", order = "002", author = "SinD")
public class DatabaseChangeLog001 {

    @Execution
    public void execution(final MongoTemplate mongoTemplate) {
        final var query = new Query(Criteria.where("username").is("sind"));
        final var exists = mongoTemplate.exists(query, UserDocument.class);

        if (BooleanUtils.isFalse(exists)) {
            final var adminDetail = UserDetail.builder()
                    .bio("System Administrator")
                    .enableNotify(true)
                    .build();

            final var adminUser = UserDocument.builder()
                    .username("sind")
                    .displayName("System Admin")
                    .email("norsindark@gmail.com")
                    .password("Aa@123456789")
                    .status(UserStatusEnum.ACTIVE)
                    .detail(adminDetail)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            mongoTemplate.save(adminUser);
        }
    }

    @RollbackExecution
    public void rollback(final MongoTemplate mongoTemplate) {
        final var query = new Query(Criteria.where("username").is("sind"));
        mongoTemplate.remove(query, UserDocument.class);
    }
}
