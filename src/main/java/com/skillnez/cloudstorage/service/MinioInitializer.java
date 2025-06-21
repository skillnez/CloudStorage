package com.skillnez.cloudstorage.service;

import com.skillnez.cloudstorage.exception.MinioOperationException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
public class MinioInitializer {

    @Value("${minio.bucket-name}")
    private String bucketName;

    private final MinioClient minioClient;

    @Autowired
    public MinioInitializer(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @PostConstruct
    public void init() {
        createBucket(bucketName);
    }

    private void createBucket(String bucketName){
        try {
            boolean bucketExists =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("Bucket creation error: " + bucketName, e);
        }
    }
}
