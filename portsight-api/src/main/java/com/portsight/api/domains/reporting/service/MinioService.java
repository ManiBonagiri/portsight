package com.portsight.api.domains.reporting.service;

import io.minio.*;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioService(
            @Value("${app.minio.url}") String url,
            @Value("${app.minio.access-key}") String accessKey,
            @Value("${app.minio.secret-key}") String secretKey,
            @Value("${app.minio.bucket-name}") String bucketName) {

        this.bucketName = bucketName;
        this.minioClient = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    @PostConstruct
    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket '{}' created", bucketName);
            } else {
                log.info("MinIO bucket '{}' already exists", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to initialise MinIO bucket '{}': {}", bucketName, e.getMessage());
        }
    }

    /**
     * Upload a PDF byte array to MinIO.
     * Returns the object path stored.
     */
    public String uploadReport(String objectName, byte[] pdfBytes) throws Exception {
        try (InputStream is = new ByteArrayInputStream(pdfBytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(is, pdfBytes.length, -1)
                            .contentType("application/pdf")
                            .build());
            log.info("Uploaded report to MinIO: {}/{}", bucketName, objectName);
            return objectName;
        }
    }

    /**
     * Download a report from MinIO as byte array.
     */
    public byte[] downloadReport(String objectName) throws Exception {
        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build())) {
            return is.readAllBytes();
        }
    }
}