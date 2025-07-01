package com.skillnez.cloudstorage.service;

import com.skillnez.cloudstorage.dto.StorageInfoResponseDto;
import com.skillnez.cloudstorage.exception.BadPathFormatException;
import com.skillnez.cloudstorage.exception.FolderAlreadyExistsException;
import com.skillnez.cloudstorage.exception.MinioOperationException;
import com.skillnez.cloudstorage.exception.NoParentFolderException;
import com.skillnez.cloudstorage.utils.FolderTraversalMode;
import com.skillnez.cloudstorage.utils.PathUtils;
import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private final MinioClientService minioClientService;

    @Autowired
    public FileSystemService(MinioClientService minioClientService) {
        this.minioClientService = minioClientService;
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
        minioClientService.putObject(backendPath, EMPTY_STREAM, FOLDER_SIZE, DEFAULT_CONTENT_TYPE);
        return PathUtils.formStorageInfoResponseDto(backendPath, null);
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
                        minioClientService.putObject(currentPath, EMPTY_STREAM, FOLDER_SIZE, DEFAULT_CONTENT_TYPE);
                    }
                }
                minioClientService.putObject(backendPathWithFileName, fileItem.getInputStream(), fileItem.getSize(), fileItem.getContentType());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            uploadedElements.add(PathUtils.formStorageInfoResponseDto(backendPathWithFileName, fileItem.getSize()));
        }
        return uploadedElements;
    }

    public List<StorageInfoResponseDto> getElementsInFolder(String backendPath, Long userId) {
        if (!isFileOrFolderExists(backendPath) && !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        Iterable<Result<Item>> results = minioClientService.listObjects(backendPath, FolderTraversalMode.NON_RECURSIVE);
        return mapMinioObjects(backendPath, results);
    }

    public List<StorageInfoResponseDto> searchElements(String backendPath, String query, Long userId) {
        if (!isFileOrFolderExists(backendPath) && !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        List<StorageInfoResponseDto> searchResults = new ArrayList<>();
        Iterable<Result<Item>> results = minioClientService.listObjects(backendPath, FolderTraversalMode.RECURSIVE);
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

    public StorageInfoResponseDto getElement(String backendPath, Long userId) {
        if (!isFileOrFolderExists(backendPath) && !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        StorageInfoResponseDto element = null;
        try {
            StatObjectResponse statObject = minioClientService.statObject(backendPath);
            element = PathUtils.formStorageInfoResponseDto(backendPath, statObject.size());
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                throw new NoSuchElementException("No element found");
            }
        }
        if (element == null) {
            throw new NoSuchElementException("No element found");
        }
        return element;
    }

    public void deleteFile(String backendPath, Long userId) {
        if (!isFileOrFolderExists(backendPath) && !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        log.info("deleted file {} from {}", Path.of(backendPath).getFileName().toString(), backendPath);
        minioClientService.removeObject(backendPath);
    }

    public void deleteFolder(String backendPath, Long userId) {
        if (!isFileOrFolderExists(backendPath) && !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        removeFolder(backendPath);
    }

    public InputStreamResource downloadFile(String backendPath, Long userId) {
        if (!isFileOrFolderExists(backendPath) && !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        InputStream downloadStream;
        downloadStream = minioClientService.getObject(backendPath);
        if (downloadStream == null) {
            throw new NoSuchElementException("No element found");
        }
        return new InputStreamResource(downloadStream);
    }

    public StorageInfoResponseDto moveOrRenameFile(String backendPathFrom, String backendPathTo, Long userId) {
        if (!isFileOrFolderExists(backendPathFrom) && !backendPathFrom.equals("user-" + userId + "-files/")) {
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
        minioClientService.copyObject(backendPathFrom, backendPathToWithExtension);
        minioClientService.removeObject(backendPathFrom);
        return getElement(backendPathToWithExtension, userId);
    }

    public StorageInfoResponseDto moveOrRenameFolder(String backendPathFrom, String backendPathTo, Long userId) {
        if (!isFileOrFolderExists(backendPathFrom) && !backendPathFrom.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path 'from' does not exist");
        }
        if (!isFileOrFolderExists(PathUtils.removeFileOrFolderName(backendPathTo)) & !backendPathTo.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path 'to' does not exist");
        }
        if (isFileOrFolderExists(backendPathTo)) {
            throw new FolderAlreadyExistsException("folder already exists in path " + backendPathTo);
        }
        Iterable<Result<Item>> results = minioClientService.listObjects(backendPathFrom, FolderTraversalMode.RECURSIVE);
        List<String> copyPathBuffer = new ArrayList<>();
        for (Result<Item> result : results) {

            try {
                Item item = result.get();
                String oldFolderName = item.objectName();
                String newFolderName = oldFolderName.replaceFirst(Pattern.quote(backendPathFrom), backendPathTo);
                //EXPERIMENTAL
                if (newFolderName.startsWith(oldFolderName) && newFolderName.length() > oldFolderName.length()
                && newFolderName.endsWith("/") && oldFolderName.endsWith("/")) {
                    log.info("EXPERIMENTAL PARENT FOLDER CUT RESTRICTION");
                    throw new BadPathFormatException("Нельзя переместить папку внутрь самой себя или её потомка");
                }
                //EXPERIMENTAL
                minioClientService.copyObject(oldFolderName, newFolderName);
                copyPathBuffer.add(oldFolderName);
            } catch (IOException | GeneralSecurityException | MinioException e) {
                throw new MinioOperationException("Object listing error", e);
            }
        }
        //сделал для того, чтобы разделить операции копирования и удаления
        for (String oldPath : copyPathBuffer) {
            minioClientService.removeObject(oldPath);
        }
        copyPathBuffer.clear();
        return getElement(backendPathTo, userId);
    }

    public void downloadFolder(String backendPath, Long userId, OutputStream outputStream) {
        if (!isFileOrFolderExists(backendPath) & !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("path does not exist");
        }
        try (ZipOutputStream zipArchive = new ZipOutputStream(outputStream)) {
            Iterable<Result<Item>> results = minioClientService.listObjects(backendPath, FolderTraversalMode.RECURSIVE);
            for (Result<Item> result : results) {
                Item item = result.get();
                try (InputStream minioDownloadStream = minioClientService.getObject(item.objectName())) {
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

    private boolean isFileOrFolderExists(String path) {
        try {
            minioClientService.statObject(path);
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
        }
        return true;
    }

    private boolean isParentFolderExists(String backendPath) {
        backendPath = PathUtils.normalizePath(backendPath);
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

    private void removeFolder(String backendPath) {
        Iterable<Result<Item>> results = minioClientService.listObjects(backendPath, FolderTraversalMode.RECURSIVE);
        for (Result<Item> result : results) {
            try {
                Item item = result.get();
                minioClientService.removeObject(item.objectName());
            } catch (IOException | GeneralSecurityException | MinioException e) {
                throw new MinioOperationException("Object listing error: " + backendPath, e);
            }
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
}
