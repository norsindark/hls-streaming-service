package com.hls.streaming.media.domain.enums;

public enum UploadStep {
    INIT_MULTIPART,
    UPLOADING,
    COMPLETE_MULTIPART,
    FFMPEG_TRANSCODING,
    HLS_UPLOAD,
    COMPLETED,
    FAILED
}
