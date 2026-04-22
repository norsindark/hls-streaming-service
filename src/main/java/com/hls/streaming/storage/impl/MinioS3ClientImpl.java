package com.hls.streaming.storage.impl;

import com.hls.streaming.config.properties.StorageConfig;
import com.hls.streaming.storage.S3Client;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MinioS3ClientImpl implements S3Client {

    private final software.amazon.awssdk.services.s3.S3Client awsS3Client;
    private final S3Presigner s3Presigner;
    private final StorageConfig storageConfig;

    @Override
    public String uploadFile(final String folder, final String fileName,
            final InputStream inputStream, final String contentType, final long size) {
        if (StringUtils.isBlank(folder) || StringUtils.isBlank(fileName) || Objects.isNull(inputStream)) {
            throw new IllegalArgumentException();
        }

        var key = folder + "/" + fileName;

        var requestBuilder = PutObjectRequest.builder()
                .bucket(storageConfig.getBucketName())
                .key(key)
                .acl(ObjectCannedACL.PUBLIC_READ);

        if (StringUtils.isNotBlank(contentType)) {
            requestBuilder.contentType(contentType);
        }

        awsS3Client.putObject(requestBuilder.build(), RequestBody.fromInputStream(inputStream, size));

        return key;
    }

    @Override
    public String generatePresignedUrl(final String key, final HttpMethod method) {
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException();
        }

        if (HttpMethod.PUT.equals(method)) {
            var putObjectRequest = PutObjectRequest.builder()
                    .bucket(storageConfig.getBucketName())
                    .key(key)
                    .build();

            var presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofDays(1))
                    .putObjectRequest(putObjectRequest)
                    .build();

            return s3Presigner.presignPutObject(presignRequest).url().toString();
        }

        var getObjectRequest = software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                .bucket(storageConfig.getBucketName())
                .key(key)
                .build();

        var presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofDays(1))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public void deleteFolder(final String folderPrefix) {
        if (StringUtils.isBlank(folderPrefix)) {
            return;
        }

        var listRequest = ListObjectsV2Request.builder()
                .bucket(storageConfig.getBucketName())
                .prefix(folderPrefix)
                .build();

        var response = awsS3Client.listObjectsV2(listRequest);

        for (var s3Object : response.contents()) {
            var deleteRequest = DeleteObjectRequest.builder()
                    .bucket(storageConfig.getBucketName())
                    .key(s3Object.key())
                    .build();
            awsS3Client.deleteObject(deleteRequest);
        }
    }

    @Override
    public void downloadFile(final String key, final Path destination) {
        if (StringUtils.isBlank(key) || Objects.isNull(destination)) {
            throw new IllegalArgumentException();
        }

        var getObjectRequest = GetObjectRequest.builder()
                .bucket(storageConfig.getBucketName())
                .key(key)
                .build();

        awsS3Client.getObject(getObjectRequest, destination);
    }
}
