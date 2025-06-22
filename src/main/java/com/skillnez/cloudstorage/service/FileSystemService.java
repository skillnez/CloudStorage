package com.skillnez.cloudstorage.service;

import com.skillnez.cloudstorage.dto.ResourceType;
import com.skillnez.cloudstorage.dto.StorageInfoResponseDto;
import com.skillnez.cloudstorage.exception.BadPathFormatException;
import com.skillnez.cloudstorage.exception.FolderAlreadyExistsException;
import com.skillnez.cloudstorage.exception.MinioOperationException;
import com.skillnez.cloudstorage.exception.NoParentFolderException;
import com.skillnez.cloudstorage.utils.PathFactory;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileSystemService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Autowired
    public FileSystemService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public void createFolder (String fullNormalizedPath) {
        if (!fullNormalizedPath.endsWith("/")){
            throw new BadPathFormatException("folder name must end with /");
        }
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fullNormalizedPath)
                    .stream(new ByteArrayInputStream(new byte[] {}), 0, -1)
                    .contentType("application/octet-stream")
                    .build());
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("folder creation error: " + fullNormalizedPath, e);
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

    public void checkFolderAlreadyExists (String fullNormalizedPath) {
        if (hasAnyFolderWithPrefix(fullNormalizedPath)) {
            throw new FolderAlreadyExistsException("file or folder already exists: " + fullNormalizedPath);
        }
    }

    public void checkFolderExists (String fullNormalizedPath) {
        if (!hasAnyFolderWithPrefix(fullNormalizedPath)) {
            throw new NoParentFolderException("folder does not exist: " + fullNormalizedPath);
        }
    }

    public void checkParentFolders(String fullNormalizedPath) {
        String[] pathPrefix = fullNormalizedPath.split("/");
        if (pathPrefix.length <= 2) {
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < pathPrefix.length - 1; i++) {
                String currentPath = stringBuilder.append(pathPrefix[i]).append("/").toString();
                if (!hasAnyFolderWithPrefix(currentPath)) {
                    throw new NoParentFolderException("path does not exist");
                }
        }
    }

    public List<StorageInfoResponseDto> getElementsInFolder(String fullNormalizedPath) {
        List<StorageInfoResponseDto> elementsInFolder = new ArrayList<>();
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fullNormalizedPath)
                        .recursive(false)
                        .build()
        );
        for (Result<Item> r : results) {
            try {
                Item item = r.get();
                String path = item.objectName();
                if (path.equals(fullNormalizedPath)) {
                    continue;
                }
                String name = PathFactory.getFileOrFolderName(item.objectName());
                Long size = (item.objectName().endsWith("/")) ? null : item.size();
                ResourceType resourceType = (item.objectName().endsWith("/")) ? ResourceType.DIRECTORY : ResourceType.FILE;
                elementsInFolder.add(new StorageInfoResponseDto(path, name, size, resourceType ));
            } catch (IOException | GeneralSecurityException | MinioException e) {
                throw new MinioOperationException("Object listing error: " + fullNormalizedPath, e);
            }
        }
        return elementsInFolder;
    }

    public List<StorageInfoResponseDto> upload (String fullNormalizedPath, MultipartFile[] file) {
        List<StorageInfoResponseDto> uploadedElements = new ArrayList<>();
        for (MultipartFile fileItem : file) {
            String fileName = PathFactory.normalizePath(fileItem.getOriginalFilename());
            int i = 123;
            String fullNormalizedPathWithFileName = PathFactory.addFilenamePrefix(fullNormalizedPath, fileName);
            //ну чтобы точно с путем нельзя было накосячить
            fullNormalizedPathWithFileName = PathFactory.normalizePath(fullNormalizedPathWithFileName);
            checkFolderAlreadyExists(fullNormalizedPathWithFileName);
            try {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fullNormalizedPathWithFileName)
                                .stream(fileItem.getInputStream(), fileItem.getSize(), -1)
                                .contentType(fileItem.getContentType())
                                .build()
                );
                String name = PathFactory.getFileOrFolderName(fullNormalizedPathWithFileName);
                Long size = (fullNormalizedPathWithFileName.endsWith("/")) ? null : fileItem.getSize();
                ResourceType resourceType = (fullNormalizedPathWithFileName.endsWith("/")) ? ResourceType.DIRECTORY : ResourceType.FILE;
                uploadedElements.add(new StorageInfoResponseDto(fullNormalizedPathWithFileName, name, size, resourceType ));
            } catch (IOException | GeneralSecurityException | MinioException e) {
                throw new MinioOperationException("Object upload error: " + fullNormalizedPath, e);
            }
        }
        return uploadedElements;
    }

}
