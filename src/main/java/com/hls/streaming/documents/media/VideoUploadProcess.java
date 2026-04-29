package com.hls.streaming.documents.media;

import com.hls.streaming.enums.UploadProcess;
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
import java.util.Map;

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

    /**
     * Current step
     * CREATED -> UPLOADING -> MERGING -> PROCESSING -> UPLOADING_HLS -> DONE -> FAILED
     */
    @Builder.Default
    @Field("status")
    private UploadProcess status = UploadProcess.CREATED;

    /**
     * % progress (0 - 100)
     */
    @Builder.Default
    @Field("progress")
    private Integer progress = 0;

    /**
     * Current step name (debug UI)
     * ex: "upload_part_3", "ffmpeg_transcoding", ...
     */
    @Field("step")
    private String step;

    /**
     * Error message if failed
     */
    @Field("error_message")
    private String errorMessage;

    /**
     * Stacktrace (optional)
     */
    @Field("error_stack")
    private String errorStack;

    /**
     * Retry count
     */
    @Builder.Default
    @Field("retry_count")
    private Integer retryCount = 0;

    /**
     * Metadata for debugging (flexible)
     * ex: partNumber, uploadId, ffmpegTime, ...
     */
    @Field("metadata")
    private Map<String, Object> metadata;

    /**
     * Last successful step
     */
    @Field("last_success_step")
    private String lastSuccessStep;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
