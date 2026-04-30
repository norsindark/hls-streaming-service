package com.hls.streaming.media.domain.document;

import com.hls.streaming.media.domain.enums.UploadPartStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("video_upload_part")
@CompoundIndex(name = "idx_process_part", def = "{'process_id':1,'part_number':1}", unique = true)
public class VideoUploadPart {

    @Id
    private String id;

    @Indexed
    @Field("process_id")
    private String processId;

    @Field("part_number")
    private Integer partNumber;

    @Field("etag")
    private String etag;

    @Field("status")
    private UploadPartStatus status;

    @Field("retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
