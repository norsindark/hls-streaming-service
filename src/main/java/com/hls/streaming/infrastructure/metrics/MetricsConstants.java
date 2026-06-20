package com.hls.streaming.infrastructure.metrics;

public final class MetricsConstants {

    private MetricsConstants() {}

    public static final class Hls {
        private Hls() {}

        public static final String PLAYLIST_REQUEST = "hls.playlist.request";
        public static final String SEGMENT_REQUEST = "hls.segment.request";
        public static final String SEGMENT_GENERATION = "hls.segment.generation";
    }

    public static final class Upload {
        private Upload() {}

        public static final String SUCCESS = "hls.upload.success";
        public static final String FAILURE = "hls.upload.failure";
        public static final String LATENCY = "hls.upload.latency";
    }

    public static final class Multipart {
        private Multipart() {}

        public static final String INIT = "hls.multipart.init";
        public static final String COMPLETE = "hls.multipart.complete";
        public static final String ABORT = "hls.multipart.abort";
        public static final String COMPLETE_LATENCY = "hls.multipart.complete.latency";
    }

    public static final class Tags {
        private Tags() {}

        public static final String OUTCOME = "outcome";
        public static final String TYPE = "type";

        public static final String SUCCESS = "success";
        public static final String FAILURE = "failure";
    }
}
