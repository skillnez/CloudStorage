package com.skillnez.cloudstorage.service;

import com.skillnez.cloudstorage.dto.ResourceType;
import com.skillnez.cloudstorage.dto.StorageInfoResponseDto;
import com.skillnez.cloudstorage.exception.BadPathFormatException;
import org.springframework.stereotype.Service;

@Service
public class PathService {

    private static final String EMPTY_STRING = "";

    public String formatPathForBackend(String rawPath, Long userId) {
        String normalizedPath = normalizePath(rawPath);
        return addUserRootFolder(normalizedPath, userId);
    }

    public String formatPathForUpload(String backendPath, String fileName) {
        String normalizedFilename = normalizePath(fileName);
        return addAppendixToPath(backendPath, normalizedFilename);
    }

    public StorageInfoResponseDto formStorageInfoResponseDto (String backendPath, Long fileSize) {
        String path = removeUserRootFolder(backendPath);
        String name = getFileOrFolderName(backendPath);
        if (name.equals(path)) {
            path = "";
        }
        Long size = (name.endsWith("/")) ? null : fileSize;
        ResourceType resourceType = (name.endsWith("/")) ? ResourceType.DIRECTORY : ResourceType.FILE;
        return new StorageInfoResponseDto(removeFileOrFolderName(path), name, size, resourceType);
    }

    private String normalizePath(String path) {
        if (path.matches(".*[:*?\"<>|].*")) {
            throw new BadPathFormatException("path cannot contain ':  *  ?  \"  <  >  |'" );
        }
        String cleaned = removeTrailingSlash(path);
        if (cleaned.contains("..")) {
            throw new BadPathFormatException("path cannot contain .. in row");
        }
        return cleaned;
    }

    private String addUserRootFolder(String rawPath, Long id) {
        return String.format("user-%s-files/%s", id, rawPath);
    }

    private String addAppendixToPath(String backendPath, String appendix) {
        if (backendPath.endsWith("/")){
            return String.format("%s%s", backendPath, appendix);
        }
        return String.format("%s/%s", backendPath, appendix);
    }

    private String getFileOrFolderName (String backendPath) {
        int slashIndex;
        if (backendPath.endsWith("/")) {
            slashIndex = backendPath.lastIndexOf('/', backendPath.length() - 2);
        } else
        {
            slashIndex = backendPath.lastIndexOf('/');
        }
        if (slashIndex == -1) {
            throw new BadPathFormatException("Filename can't be blank");
        }
        return backendPath.substring(slashIndex + 1);
    }

    private String removeFileOrFolderName(String backendPath) {
        int slashIndex;
        if (backendPath.endsWith("/")) {
            slashIndex = backendPath.lastIndexOf('/', backendPath.length() - 2);
        } else {
            slashIndex = backendPath.lastIndexOf('/');
        }
        return backendPath.substring(0, slashIndex + 1);
    }

    private String removeUserRootFolder(String backendPath) {
        return backendPath.replaceFirst("^[^/]+/", "");
    }

    private String removeTrailingSlash(String rawPath) {
        return rawPath.trim()
                .replace("\\", "/")
                .replaceAll("/{2,}", "/")
                .replaceAll("^/+", "");
    }

}
