package com.hls.streaming.media.domain.document;

import com.hls.streaming.media.domain.enums.VideoStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "hls_videos")
public class Video implements Serializable {

    @Serial
    private static final long serialVersionUID = -79334098665055842L;

    @Id
    private String id;

    @Version
    private Long version;

    @Indexed
    @Field("user_id")
    private String userId;

    @Indexed
    @Field("process_id")
    private String processId;

    @Field("title")
    private String title;

    @Field("description")
    private String description;

    @Field("folder")
    private String folder;

    @Field("file_name")
    private String fileName;

    @Field("content_type")
    private String contentType;

    @Field("file_size")
    private Long fileSize;

    @Field("hls_url")
    private String hlsUrl;

    @Field("thumbnail_url")
    private String thumbnailUrl;

    @Field("duration")
    private Double duration;

    @Builder.Default
    @Field("status")
    private VideoStatus status = VideoStatus.CREATED;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
