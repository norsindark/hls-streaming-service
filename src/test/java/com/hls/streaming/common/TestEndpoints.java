package com.hls.streaming.common;

public final class TestEndpoints {

    private TestEndpoints() {}

    public static final class Public {
        public static final String GET_VIDEO = "/api/v1/videos/{id}";
    }

    public static final class Auth {
        public static final String MY_VIDEOS = "/api/v1/videos/my-videos";
        public static final String BY_USER = "/api/v1/videos/user/{userId}";
    }
}
