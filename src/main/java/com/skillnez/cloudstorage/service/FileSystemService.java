package com.skillnez.cloudstorage.service;

import com.skillnez.cloudstorage.exception.FolderAlreadyExistsException;
import com.skillnez.cloudstorage.exception.MinioOperationException;
import com.skillnez.cloudstorage.exception.NoParentFolderException;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
public class FileSystemService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Autowired
    public FileSystemService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public void createFolder (String path) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .stream(new ByteArrayInputStream(new byte[0]), 0, 0)
                    .contentType("application/octet-stream")
                    .build());
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("folder creation error: " + path, e);
        }
    }

    public boolean hasAnyFolderWithPrefix (String prefix) {
        Iterable<Result<Item>> items = minioClient.listObjects(
                ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .recursive(true)
                .maxKeys(1)
                        .build());
        return items.iterator().hasNext();
    }

    private void checkFolderAlreadyExists (String fullNormalizedPath) {
        if (hasAnyFolderWithPrefix(fullNormalizedPath)) {
            throw new FolderAlreadyExistsException("folder already exists: " + fullNormalizedPath);
        }
    }

    public void checkParentFolders(String fullNormalizedPath) {
        checkFolderAlreadyExists(fullNormalizedPath);
        String[] pathPrefix = fullNormalizedPath.split("/");
        if (pathPrefix.length <= 2) {
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < pathPrefix.length - 1; i++) {
                String currentPath = stringBuilder.append(pathPrefix[i]).append("/").toString();
                if (!hasAnyFolderWithPrefix(currentPath)) {
                    throw new NoParentFolderException("there's no folder with prefix: " + currentPath);
                }
        }
    }
}
