package com.hls.streaming.storage;

import org.springframework.http.HttpMethod;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public interface S3Client {

    String uploadFile(String folder, String fileName, InputStream inputStream, String contentType, long size);

    String generatePresignedUrl(String key, HttpMethod method);

    String createMultipartUpload(String key, String contentType);

    String generatePresignedUploadPartUrl(String key, String uploadId, int partNumber);

    void completeMultipartUpload(String key, String uploadId, List<CompletedPart> parts);

    void deleteFolder(String folderPrefix);

    void downloadFile(String key, Path destination);

    void abortMultipartUpload(String key, String uploadId);
}
