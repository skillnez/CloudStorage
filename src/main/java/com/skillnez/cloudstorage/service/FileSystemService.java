package com.skillnez.cloudstorage.service;

import com.skillnez.cloudstorage.dto.StorageInfoResponseDto;
import com.skillnez.cloudstorage.exception.BadPathFormatException;
import com.skillnez.cloudstorage.exception.FolderAlreadyExistsException;
import com.skillnez.cloudstorage.exception.MinioOperationException;
import com.skillnez.cloudstorage.exception.NoParentFolderException;
import com.skillnez.cloudstorage.utils.FolderTraversalMode;
import com.skillnez.cloudstorage.utils.PathUtils;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        if (isFileOrFolderExists(backendPath)) {
            throw new FolderAlreadyExistsException("file or folder already exists");
        }
        if (!isParentFolderExists(backendPath)) {
            throw new NoParentFolderException("parent folder does not exist");
        }
        log.info("folder: {} created", backendPath);
        return putObjectInStorage(backendPath, EMPTY_STREAM, FOLDER_SIZE, DEFAULT_CONTENT_TYPE);
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
            if (isFileOrFolderExists(backendPathWithFileName)) {
                throw new FolderAlreadyExistsException("file or folder already exists");
            }
            try {
                String[] pathPrefix = backendPathWithFileName.split("/");
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < pathPrefix.length - 1; i++) {
                    String currentPath = stringBuilder.append(pathPrefix[i]).append("/").toString();
                    if (!isFileOrFolderExists(currentPath)) {
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
    public List<StorageInfoResponseDto> getElementsInFolder(String backendPath, Long userId) {
        if (!isFileOrFolderExists(backendPath) & !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        Iterable<Result<Item>> results = listMinioObjects(backendPath, FolderTraversalMode.NON_RECURSIVE);
        return mapMinioObjects(backendPath, results);
    }
    public List<StorageInfoResponseDto> searchElements(String backendPath, String query, Long userId) {
        if (!isFileOrFolderExists(backendPath) & !backendPath.equals("user-" + userId + "-files/")) {
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

    public StorageInfoResponseDto getElement (String backendPath, Long userId) {
        if (!isFileOrFolderExists(backendPath) & !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        StorageInfoResponseDto element = null;
        try {
            StatObjectResponse statObject = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(backendPath)
                            .build()
            );
            element = PathUtils.formStorageInfoResponseDto(backendPath, statObject.size());
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                throw new NoSuchElementException("No element found");
            }
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("Unexpected minio error", e);
        }
        if (element == null) {
            throw new NoSuchElementException("No element found");
        }
        return element;
    }

    public void deleteFile (String backendPath, Long userId) {
        if (!isFileOrFolderExists(backendPath) & !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        log.info("deleted file {} from {}", Path.of(backendPath).getFileName().toString(), backendPath);
        removeFileFromMinio(backendPath);
    }

    public void deleteFolder(String backendPath, Long userId) {
        if (!isFileOrFolderExists(backendPath) & !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        removeFolderFromMinio(backendPath);
    }

    public InputStreamResource downloadFile (String backendPath, Long userId) {
        if (!isFileOrFolderExists(backendPath) & !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        InputStream downloadStream;
        downloadStream = getInputStreamFromMinioObject(backendPath);
        if (downloadStream == null) {
            throw new NoSuchElementException("No element found");
        }
        return new InputStreamResource(downloadStream);
    }

    public StorageInfoResponseDto moveOrRenameFile(String backendPathFrom, String backendPathTo, Long userId) {
        if (!isFileOrFolderExists(backendPathFrom) & !backendPathFrom.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path 'from' does not exist");
        }
        if (!isFileOrFolderExists(PathUtils.removeFileOrFolderName(backendPathTo)) & !backendPathTo.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path 'to' does not exist");
        }
        String extension = PathUtils.getExtension(backendPathFrom, backendPathTo);
        String backendPathToWithExtension = PathUtils.normalizePath(backendPathTo + extension);
        if (isFileOrFolderExists(backendPathToWithExtension)) {
            throw new FolderAlreadyExistsException("file already exists in path " + backendPathToWithExtension);
        }
        try {
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(bucketName)
                        .object(backendPathToWithExtension)
                        .source(CopySource.builder()
                                .bucket(bucketName)
                                .object(backendPathFrom)
                                .build())
                        .build());
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("Minio copy error", e);
        }
        removeFileFromMinio(backendPathFrom);
        return getElement(backendPathToWithExtension, userId);
    }

    public StorageInfoResponseDto moveOrRenameFolder(String backendPathFrom, String backendPathTo, Long userId) {
        if (!isFileOrFolderExists(backendPathFrom) & !backendPathFrom.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path 'from' does not exist");
        }
        if (!isFileOrFolderExists(PathUtils.removeFileOrFolderName(backendPathTo)) & !backendPathTo.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path 'to' does not exist");
        }
        if (isFileOrFolderExists(backendPathTo)) {
            throw new FolderAlreadyExistsException("folder already exists in path " + backendPathTo);
        }
        Iterable<Result<Item>> results = listMinioObjects(backendPathFrom, FolderTraversalMode.RECURSIVE);
        for (Result<Item> result : results) {
            try {
                Item item = result.get();
                String oldFolderName = item.objectName();
                String newFolderName = oldFolderName.replaceFirst(Pattern.quote(backendPathFrom), backendPathTo);
                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(bucketName)
                                .object(newFolderName)
                                .source(CopySource.builder()
                                        .bucket(bucketName)
                                        .object(oldFolderName)
                                        .build())
                                .build());
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(oldFolderName)
                                .build());
            } catch (IOException | GeneralSecurityException | MinioException e) {
                throw new MinioOperationException("Object listing error", e);
            }
        }
        return getElement(backendPathTo, userId);
    }

    public void downloadFolder (String backendPath, Long userId, OutputStream outputStream) {
        if (!isFileOrFolderExists(backendPath) & !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        try (ZipOutputStream zipArchive = new ZipOutputStream(outputStream)) {
            Iterable<Result<Item>> results = listMinioObjects(backendPath, FolderTraversalMode.RECURSIVE);
            for (Result<Item> result : results) {
                Item item = result.get();
                try (InputStream minioDownloadStream = getInputStreamFromMinioObject(item.objectName())) {
                    String entryName = item.objectName().substring(("user-" + userId + "-files/").length());
                    zipArchive.putNextEntry(new ZipEntry(entryName));
                    minioDownloadStream.transferTo(zipArchive);
                    zipArchive.closeEntry();
                }
            }
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("Object listing error: " + backendPath, e);
        }
    }

    private StorageInfoResponseDto putObjectInStorage(String backendPath, InputStream file, long fileSize,
                                                      String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(backendPath)
                            .stream(file, fileSize, -1)
                            .contentType(contentType).build());
            return PathUtils.formStorageInfoResponseDto(backendPath, null);
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("folder creation error: " + backendPath, e);
        }
    }

    private boolean isFileOrFolderExists(String path) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isParentFolderExists(String backendPath) {
        String[] pathPrefix = backendPath.split("/");
        if (pathPrefix.length <= 2) {
            return true;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < pathPrefix.length - 1; i++) {
            String currentPath = stringBuilder.append(pathPrefix[i]).append("/").toString();
            if (!isFileOrFolderExists(currentPath)) {
                return false;
            }
        }
        return true;
    }

    private InputStream getInputStreamFromMinioObject(String backendPath) {
        InputStream downloadStream;
        try {
            downloadStream = minioClient.getObject(
                    GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(backendPath)
                    .build());
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("Object listing error: " + backendPath, e);
        }
        return downloadStream;
    }

    private void removeFolderFromMinio(String backendPath) {
        Iterable<Result<Item>> results = listMinioObjects(backendPath, FolderTraversalMode.RECURSIVE);
        for (Result<Item> result : results) {
            try {
                Item item = result.get();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(item.objectName())
                                .build());
            } catch (IOException | GeneralSecurityException | MinioException e) {
                throw new MinioOperationException("Object listing error: " + backendPath, e);
            }
        }
    }

    private void removeFileFromMinio(String backendPath) {
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
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(backendPath)
                        .recursive(searchMode)
                        .build());
    }
}
