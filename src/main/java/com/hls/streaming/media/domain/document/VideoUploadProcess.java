package com.hls.streaming.media.domain.document;

import com.hls.streaming.media.domain.enums.UploadProcess;
import com.hls.streaming.media.domain.enums.UploadStep;
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
@Document(collection = "video_upload_process")
public class VideoUploadProcess implements Serializable {

    @Serial
    private static final long serialVersionUID = 912391239123L;

    @Id
    private String id;

    @Version
    private Long version;

    @Indexed
    @Field("video_id")
    private String videoId;

    @Indexed
    @Field("user_id")
    private String userId;

    @Indexed
    @Field("status")
    @Builder.Default
    private UploadProcess status = UploadProcess.CREATED;

    @Field("step")
    @Builder.Default
    private UploadStep step = UploadStep.INIT_MULTIPART;

    @Field("error_message")
    private String errorMessage;

    @Field("error_stack")
    private String errorStack;

    @Builder.Default
    @Field("retry_count")
    private Integer retryCount = 0;

    @Field("upload_id")
    private String uploadId;

    @Field("total_parts")
    private Integer totalParts;

    @Field("last_success_step")
    private UploadStep lastSuccessStep;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
