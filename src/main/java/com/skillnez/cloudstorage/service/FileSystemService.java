package com.skillnez.cloudstorage.service;

import com.skillnez.cloudstorage.dto.StorageInfoResponseDto;
import com.skillnez.cloudstorage.exception.BadPathFormatException;
import com.skillnez.cloudstorage.exception.FolderAlreadyExistsException;
import com.skillnez.cloudstorage.exception.MinioOperationException;
import com.skillnez.cloudstorage.exception.NoParentFolderException;
import com.skillnez.cloudstorage.utils.FolderTraversalMode;
import com.skillnez.cloudstorage.utils.PathUtils;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FileSystemService {

    private static final ByteArrayInputStream EMPTY_STREAM = new ByteArrayInputStream(new byte[]{});
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final Long FOLDER_SIZE = 0L;
    private final MinioClient minioClient;
    @Value("${minio.bucket-name}")
    private String bucketName;

    @Autowired
    public FileSystemService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public StorageInfoResponseDto createFolder(String backendPath) {
        if (!backendPath.endsWith("/")) {
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
        Iterable<Result<Item>> items = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).prefix(prefix).recursive(true).maxKeys(1).build());
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
    public List<StorageInfoResponseDto> upload(String backendPath, MultipartFile[] file) {
        int skippedFiles = 0;
        List<StorageInfoResponseDto> uploadedElements = new ArrayList<>();
        for (MultipartFile fileItem : file) {
            if (fileItem.getOriginalFilename() == null || fileItem.getOriginalFilename().isEmpty()) {
                skippedFiles++;
                log.warn("{} files were skipped. Cause: filename is blank or null", skippedFiles);
                continue;
            }
            String backendPathWithFileName = PathUtils.formatPathForUpload(backendPath, fileItem.getOriginalFilename());
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
            uploadedElements.add(PathUtils.formStorageInfoResponseDto(backendPathWithFileName, fileItem.getSize()));
        }
        return uploadedElements;
    }

    private StorageInfoResponseDto putObjectInStorage(String backendPath, InputStream file, long fileSize, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(backendPath).stream(file, fileSize, -1).contentType(contentType).build());
            return PathUtils.formStorageInfoResponseDto(backendPath, null);
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("folder creation error: " + backendPath, e);
        }
    }

    public List<StorageInfoResponseDto> getElementsInFolder(String backendPath, Long userId) {
        if (!isFolderExists(backendPath) & !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        Iterable<Result<Item>> results = listMinioObjects(backendPath, FolderTraversalMode.NON_RECURSIVE);
        return mapMinioObjects(backendPath, results);
    }

    public List<StorageInfoResponseDto> searchElements(String backendPath, String query, Long userId) {
        if (!isFolderExists(backendPath) & !backendPath.equals("user-" + userId + "-files/")) {
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
                StorageInfoResponseDto fileToSort = PathUtils.formStorageInfoResponseDto(item.objectName(), item.size());
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
                elementsInFolder.add(PathUtils.formStorageInfoResponseDto(item.objectName(), item.size()));
            } catch (IOException | GeneralSecurityException | MinioException e) {
                throw new MinioOperationException("Object listing error: " + backendPath, e);
            }
        }
        return elementsInFolder;
    }

    private Iterable<Result<Item>> listMinioObjects(String backendPath, FolderTraversalMode traversalMode) {
        boolean searchMode = (FolderTraversalMode.RECURSIVE == traversalMode);
        return minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).prefix(backendPath).recursive(searchMode).build());
    }
}
