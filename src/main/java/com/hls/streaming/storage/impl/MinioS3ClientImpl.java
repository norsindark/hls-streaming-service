package com.hls.streaming.storage.impl;

import com.hls.streaming.config.properties.StorageConfig;
import com.hls.streaming.storage.S3Client;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MinioS3ClientImpl implements S3Client {

    private final software.amazon.awssdk.services.s3.S3Client awsS3Client;
    private final S3Presigner s3Presigner;
    private final StorageConfig storageConfig;

    @PostConstruct
    public void initBucket() {
        var bucket = storageConfig.getBucketName();
        try {
            awsS3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (Exception e) {
            awsS3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
    }

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
            throw new IllegalArgumentException("Key must not be blank");
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

        var getObjectRequest = GetObjectRequest.builder()
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
    public String createMultipartUpload(final String key, final String contentType) {
        try {
            var request = CreateMultipartUploadRequest.builder()
                    .bucket(storageConfig.getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            var response = awsS3Client.createMultipartUpload(request);
            return response.uploadId();

        } catch (Exception e) {
            throw new RuntimeException("Create multipart upload failed", e);
        }
    }

    @Override
    public String generatePresignedUploadPartUrl(final String key, final String uploadId, final int partNumber) {
        try {
            var uploadPartRequest = UploadPartRequest.builder()
                    .bucket(storageConfig.getBucketName())
                    .key(key)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();

            var presignRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))
                    .uploadPartRequest(uploadPartRequest)
                    .build();

            return s3Presigner.presignUploadPart(presignRequest)
                    .url()
                    .toString();

        } catch (Exception e) {
            throw new RuntimeException("Generate presigned part url failed", e);
        }
    }

    @Override
    public void completeMultipartUpload(final String key, final String uploadId,
            List<CompletedPart> parts) {

        try {
            var completedMultipart = CompletedMultipartUpload.builder()
                    .parts(parts)
                    .build();

            var request = CompleteMultipartUploadRequest.builder()
                    .bucket(storageConfig.getBucketName())
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(completedMultipart)
                    .build();

            awsS3Client.completeMultipartUpload(request);

        } catch (Exception e) {
            throw new RuntimeException("Complete multipart upload failed", e);
        }
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
