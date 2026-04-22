package com.hls.streaming.storage;

import org.springframework.http.HttpMethod;

import java.io.InputStream;
import java.nio.file.Path;

public interface S3Client {

    String uploadFile(String folder, String fileName, InputStream inputStream, String contentType, long size);

    String generatePresignedUrl(String key, HttpMethod method);

    void deleteFolder(String folderPrefix);

    void downloadFile(String key, Path destination);
}
