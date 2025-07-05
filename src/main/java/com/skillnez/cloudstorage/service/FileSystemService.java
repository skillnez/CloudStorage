package com.skillnez.cloudstorage.service;

import com.skillnez.cloudstorage.dto.StorageInfoResponseDto;
import com.skillnez.cloudstorage.exception.*;
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
    private static final Long EMPTY_FOLDER_SIZE = 0L;
    private final MinioClientService minioClientService;

    @Autowired
    public FileSystemService(MinioClientService minioClientService) {
        this.minioClientService = minioClientService;
    }

    public void createRootFolder (Long userId) {
        String userRootFolder = "user-" + userId + "-files/";
        minioClientService.putObject(userRootFolder, EMPTY_STREAM, EMPTY_FOLDER_SIZE, DEFAULT_CONTENT_TYPE);
    }

    public StorageInfoResponseDto createFolder(String backendPath) {
        if (!backendPath.endsWith("/")) {
            throw new BadPathFormatException("Folder name must ends with /");
        }
        if (isFileOrFolderExists(backendPath)) {
            throw new FolderAlreadyExistsException("File or folder already exists");
        }
        if (!isParentFolderExists(backendPath)) {
            throw new NoParentFolderException("Parent folder does not exist");
        }
        log.info("Folder: {} created", backendPath);
        minioClientService.putObject(backendPath, EMPTY_STREAM, EMPTY_FOLDER_SIZE, DEFAULT_CONTENT_TYPE);
        return PathUtils.formStorageInfoResponseDto(backendPath, null);
    }

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
                throw new FolderAlreadyExistsException
                        ("Cant upload file, because file or folder with this name already exists in target directory");
            }
            try {
                String[] pathPrefix = backendPathWithFileName.split("/");
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < pathPrefix.length - 1; i++) {
                    String currentPath = stringBuilder.append(pathPrefix[i]).append("/").toString();
                    if (!isFileOrFolderExists(currentPath)) {
                        minioClientService.putObject(currentPath, EMPTY_STREAM, EMPTY_FOLDER_SIZE, DEFAULT_CONTENT_TYPE);
                    }
                }
                minioClientService
                        .putObject(backendPathWithFileName, fileItem.getInputStream(), fileItem.getSize(), fileItem.getContentType());
            } catch (IOException e) {
                throw new UploadErrorException("Something went wrong while uploading file");
            }
            uploadedElements.add(PathUtils.formStorageInfoResponseDto(backendPathWithFileName, fileItem.getSize()));
        }
        return uploadedElements;
    }

    public List<StorageInfoResponseDto> getElementsInFolder(String backendPath, Long userId) {
        if (!isFileOrFolderExists(backendPath) && !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("Path does not exist");
        }
        Iterable<Result<Item>> results = minioClientService.listObjects(backendPath, FolderTraversalMode.NON_RECURSIVE);
        return mapMinioObjects(backendPath, results);
    }

    public List<StorageInfoResponseDto> searchElements(String backendPath, String query, Long userId) {
        if (!isFileOrFolderExists(backendPath) && !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("Path does not exist");
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
                throw new MinioOperationException("Object listing error: ", e);
            }
        }
        return searchResults;
    }

    public StorageInfoResponseDto getElement(String backendPath, Long userId) {
        if (!isFileOrFolderExists(backendPath) && !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("Path does not exist");
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

    public void delete (String backendPath, Long userId) {
        if (backendPath.endsWith("/")) {
            deleteFolder(backendPath, userId);
            log.info("Deleted folder {} from {}", Path.of(backendPath).getFileName().toString(), backendPath);
            return;
        }
            deleteFile(backendPath, userId);
            log.info("Deleted file {} from {}", Path.of(backendPath).getFileName().toString(), backendPath);
    }

    public StorageInfoResponseDto moveOrRename (String backendPathFrom, String backendPathTo, Long userId) {
        if (backendPathFrom.endsWith("/")) {
            return moveOrRenameFolder(backendPathFrom, backendPathTo, userId);
        }
        return moveOrRenameFile(backendPathFrom, backendPathTo, userId);
    }

    public InputStream downloadFile(String backendPath, Long userId) {
        if (!isFileOrFolderExists(backendPath) && !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("Path does not exist");
        }
        InputStream downloadStream = minioClientService.getObject(backendPath);
        if (downloadStream == null) {
            throw new NoSuchElementException("No element found");
        }
        return downloadStream;
    }

    public void downloadFolder(String backendPath, Long userId, OutputStream outputStream) {
        if (!isFileOrFolderExists(backendPath) & !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("Path does not exist");
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
            log.info("Folder {} downloaded" , backendPath);
        } catch (IOException | GeneralSecurityException | MinioException e) {
            throw new MinioOperationException("Something went wrong while downloading file", e);
        }
    }

    private void deleteFolder(String backendPath, Long userId) {
        if (!isFileOrFolderExists(backendPath) && !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("Path does not exist");
        }
        removeFolder(backendPath);
    }

    private StorageInfoResponseDto moveOrRenameFile(String backendPathFrom, String backendPathTo, Long userId) {
        if (!isFileOrFolderExists(backendPathFrom) && !backendPathFrom.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("Path does not exists");
        }
        if (!isFileOrFolderExists(PathUtils.removeFileOrFolderName(backendPathTo)) &&
            !backendPathTo.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("Path does not exists");
        }
        String extension = PathUtils.getExtension(backendPathFrom, backendPathTo);
        String backendPathToWithExtension = PathUtils.normalizePath(backendPathTo + extension);
        if (isFileOrFolderExists(backendPathToWithExtension)) {
            throw new FolderAlreadyExistsException("File already exists");
        }
        minioClientService.copyObject(backendPathFrom, backendPathToWithExtension);
        minioClientService.removeObject(backendPathFrom);
        log.info("file path changed from {} to {}", backendPathFrom, backendPathTo);
        return getElement(backendPathToWithExtension, userId);
    }

    private StorageInfoResponseDto moveOrRenameFolder(String backendPathFrom, String backendPathTo, Long userId) {
        if (!isFileOrFolderExists(backendPathFrom) && !backendPathFrom.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("Path does not exists");
        }
        if (!isFileOrFolderExists(PathUtils.removeFileOrFolderName(backendPathTo))
            && !backendPathTo.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("Path does not exists");
        }
        if (isFileOrFolderExists(backendPathTo)) {
            throw new FolderAlreadyExistsException("Folder already exists in path " + backendPathTo);
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
                    throw new BadPathFormatException("Can't move parent folder to it's child folder");
                }
                //EXPERIMENTAL
                minioClientService.copyObject(oldFolderName, newFolderName);
                copyPathBuffer.add(oldFolderName);
            } catch (IOException | GeneralSecurityException | MinioException e) {
                throw new MinioOperationException("Object listing error: ", e);
            }
        }
        //сделал для того, чтобы разделить операции копирования и удаления
        for (String oldPath : copyPathBuffer) {
            minioClientService.removeObject(oldPath);
        }
        copyPathBuffer.clear();
        log.info("Folder path changed from {} to {}", backendPathFrom, backendPathTo);
        return getElement(backendPathTo, userId);
    }

    private void deleteFile(String backendPath, Long userId) {
        if (!isFileOrFolderExists(backendPath) && !backendPath.equals("user-" + userId + "-files/")) {
            throw new NoParentFolderException("Path does not exist");
        }
        minioClientService.removeObject(backendPath);
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
                throw new MinioOperationException("Object listing error: ", e);
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
                throw new MinioOperationException("Object listing error: ", e);
            }
        }
        return elementsInFolder;
    }
}
