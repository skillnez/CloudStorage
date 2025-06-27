package com.skillnez.cloudstorage.service;

import com.skillnez.cloudstorage.dto.StorageInfoResponseDto;
import com.skillnez.cloudstorage.exception.BadPathFormatException;
import com.skillnez.cloudstorage.exception.FolderAlreadyExistsException;
import com.skillnez.cloudstorage.exception.MinioOperationException;
import com.skillnez.cloudstorage.exception.NoParentFolderException;
import com.skillnez.cloudstorage.utils.FolderTraversalMode;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileSystemService {

    private final MinioClient minioClient;
    private final PathService pathService;

    private static final ByteArrayInputStream EMPTY_STREAM = new ByteArrayInputStream(new byte[] {});
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final Long FOLDER_SIZE = 0L;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Autowired
    public FileSystemService(MinioClient minioClient, PathService pathService) {
        this.minioClient = minioClient;
        this.pathService = pathService;
    }

    public StorageInfoResponseDto createFolder (String backendPath) {
        if (!backendPath.endsWith("/")){
            throw new BadPathFormatException("folder name must end with /");
        }
        if (isFolderExists(backendPath)) {
            throw new FolderAlreadyExistsException("file or folder already exists");
        }
        if (!isParentFolderExists(backendPath)) {
            throw new NoParentFolderException("parent folder does not exist");
        }
        return putObjectInStorage(backendPath, EMPTY_STREAM, FOLDER_SIZE, DEFAULT_CONTENT_TYPE);
    }

    public boolean isFolderExists(String prefix) {
        Iterable<Result<Item>> items = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .recursive(true)
                        .maxKeys(1)
                        .build());
        return items.iterator().hasNext();
    }

    public boolean isParentFolderExists(String backendPath) {
        String[] pathPrefix = backendPath.split("/");
        if (pathPrefix.length <= 2) {
            return true;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < pathPrefix.length - 1; i++) {
                String currentPath = stringBuilder.append(pathPrefix[i]).append("/").toString();
                if (!isFolderExists(currentPath)) {
                    return false;
                }
        }
        return true;
    }

    //TODO Обязательно оптимизируй создание пустых папок, выглядит дерьмово
    public List<StorageInfoResponseDto> upload (String backendPath, MultipartFile[] file) {
        List<StorageInfoResponseDto> uploadedElements = new ArrayList<>();
        for (MultipartFile fileItem : file) {
            String backendPathWithFileName = pathService.formatPathForUpload(backendPath, fileItem.getOriginalFilename());
            if (isFolderExists(backendPathWithFileName)) {
                throw new FolderAlreadyExistsException("file or folder already exists");
            }
            try {
                String[] pathPrefix = backendPathWithFileName.split("/");
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < pathPrefix.length - 1; i++) {
                    String currentPath = stringBuilder.append(pathPrefix[i]).append("/").toString();
                    if (!isFolderExists(currentPath)) {
                        putObjectInStorage(currentPath, EMPTY_STREAM, FOLDER_SIZE, DEFAULT_CONTENT_TYPE);
                    }
                }
                putObjectInStorage(backendPathWithFileName, fileItem.getInputStream(), fileItem.getSize(), fileItem.getContentType());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            uploadedElements.add(pathService.formStorageInfoResponseDto(backendPathWithFileName, fileItem.getSize()));
        }
        return uploadedElements;
    }

    private StorageInfoResponseDto putObjectInStorage(String backendPath, InputStream file, long fileSize, String contentType){
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(backendPath)
                            .stream(file, fileSize, -1)
                            .contentType(contentType)
                            .build()
            );
            return pathService.formStorageInfoResponseDto(backendPath, null);
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("folder creation error: " + backendPath, e);
        }
    }

    public List<StorageInfoResponseDto> getElementsInFolder(String backendPath, Long userId) {
        if (!isFolderExists(backendPath) & !backendPath.equals("user-"+userId+"-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        Iterable<Result<Item>> results = listMinioObjects(backendPath, FolderTraversalMode.NON_RECURSIVE);
        return mapMinioObjects(backendPath, results);
    }

    public List<StorageInfoResponseDto> searchElements(String backendPath, String query, Long userId) {
        if (!isFolderExists(backendPath) & !backendPath.equals("user-"+userId+"-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        List<StorageInfoResponseDto> searchResults = new ArrayList<>();
        Iterable<Result<Item>> results = listMinioObjects(backendPath, FolderTraversalMode.RECURSIVE);
        for (Result<Item> result : results) {
            try {
                Item item = result.get();
                if (backendPath.equals(item.objectName())) {
                    continue;
                }
                StorageInfoResponseDto fileToSort = pathService.formStorageInfoResponseDto(item.objectName(), item.size());
                if (fileToSort.getName().toLowerCase().contains(query.toLowerCase())) {
                    searchResults.add(fileToSort);
                }
            } catch (IOException | GeneralSecurityException | MinioException e) {
                throw new MinioOperationException("Object listing error: " + backendPath, e);
            }
        }
        return searchResults;
    }

    private List<StorageInfoResponseDto> mapMinioObjects(String backendPath, Iterable<Result<Item>> results) {
        List<StorageInfoResponseDto> elementsInFolder = new ArrayList<>();
        for (Result<Item> result : results) {
            try {
                Item item = result.get();
                if (backendPath.equals(item.objectName())) {
                    continue;
                }
                elementsInFolder.add(pathService.formStorageInfoResponseDto(item.objectName(), item.size()));
            } catch (IOException | GeneralSecurityException | MinioException e) {
                throw new MinioOperationException("Object listing error: " + backendPath, e);
            }
        }
        return elementsInFolder;
    }

    private Iterable<Result<Item>> listMinioObjects(String backendPath, FolderTraversalMode traversalMode) {
        boolean searchMode = (FolderTraversalMode.RECURSIVE == traversalMode);
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(backendPath)
                        .recursive(searchMode)
                        .build()
        );
    }

}
