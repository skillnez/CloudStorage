package com.skillnez.cloudstorage.service;

import com.skillnez.cloudstorage.dto.StorageInfoResponseDto;
import com.skillnez.cloudstorage.exception.MinioOperationException;
import com.skillnez.cloudstorage.utils.FolderTraversalMode;
import com.skillnez.cloudstorage.utils.PathUtils;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

@Service
public class MinioClientService {

    private final MinioClient minioClient;
    @Value("${minio.bucket-name}")
    private String bucketName;

    @Autowired
    public MinioClientService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public void copyObject(String backendPathFrom, String backendPathTo) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(backendPathTo)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(backendPathFrom)
                                    .build())
                            .build());
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("Object listing error", e);
        }
    }

    public void putObject(String backendPath, InputStream file, long fileSize,
                                             String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(backendPath)
                            .stream(file, fileSize, -1)
                            .contentType(contentType).build());
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("folder creation error: " + backendPath, e);
        }
    }

    public StatObjectResponse statObject(String backendPath) throws ErrorResponseException {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(backendPath)
                            .build()
            );
        } catch (ErrorResponseException e) {
            throw e;
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("Unexpected minio error", e);
        }
    }

    public InputStream getObject(String backendPath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(backendPath)
                            .build());
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("Object listing error: " + backendPath, e);
        }
    }

    public void removeObject(String backendPath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(backendPath)
                            .build());
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("removing has ended ", e);
        }
    }

    public Iterable<Result<Item>> listObjects(String backendPath, FolderTraversalMode traversalMode) {
        boolean searchMode = (FolderTraversalMode.RECURSIVE == traversalMode);
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(backendPath)
                        .recursive(searchMode)
                        .build());
    }
}
