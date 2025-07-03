package com.skillnez.cloudstorage.utils;

import com.skillnez.cloudstorage.dto.ResourceType;
import com.skillnez.cloudstorage.dto.StorageInfoResponseDto;
import com.skillnez.cloudstorage.exception.BadPathFormatException;

public class PathUtils {

    public static String formatPathForBackend(String rawPath, Long userId) {
        String normalizedPath = normalizePath(rawPath);
        return addUserRootFolder(normalizedPath, userId);
    }

    public static String formatPathForUpload(String backendPath, String fileName) {
        String normalizedFilename = normalizePath(fileName);
        return addAppendixToPath(backendPath, normalizedFilename);
    }

    public static StorageInfoResponseDto formStorageInfoResponseDto(String backendPath, Long fileSize) {
        if (backendPath == null || backendPath.isEmpty()) {
            throw new BadPathFormatException("Invalid path");
        }
        String normalizedPath = normalizePath(backendPath);
        normalizedPath = removeUserRootFolder(normalizedPath);
        normalizedPath = removeFileOrFolderName(normalizedPath);
        String name = getFileOrFolderName(backendPath);
        if (name.equals(normalizedPath)) {
            normalizedPath = "";
        }
        Long size = (name.endsWith("/")) ? null : fileSize;
        ResourceType resourceType = (name.endsWith("/")) ? ResourceType.DIRECTORY : ResourceType.FILE;
        return new StorageInfoResponseDto(normalizedPath, name, size, resourceType);
    }

    public static String removeFileOrFolderName(String backendPath) {
        String normalizedPath = normalizePath(backendPath);
        int slashIndex;
        if (normalizedPath.endsWith("/")) {
            slashIndex = normalizedPath.lastIndexOf('/', normalizedPath.length() - 2);
        } else {
            slashIndex = normalizedPath.lastIndexOf('/');
        }
        return normalizedPath.substring(0, slashIndex + 1);
    }

    public static String getFileOrFolderName(String backendPath) {
        String normalizedPath = normalizePath(backendPath);
        int slashIndex;
        if (normalizedPath.endsWith("/")) {
            slashIndex = normalizedPath.lastIndexOf('/', normalizedPath.length() - 2);
        } else {
            slashIndex = normalizedPath.lastIndexOf('/');
        }
        if (slashIndex == -1) {
            throw new BadPathFormatException("Filename can't be blank");
        }
        return normalizedPath.substring(slashIndex + 1);
    }

    public static String normalizePath(String path) {
        if (path.matches(".*[:*?\"<>|].*")) {
            throw new BadPathFormatException("Path cannot contain ':  *  ?  \"  <  >  |'");
        }
        String cleaned = removeTrailingSlash(path);
        if (cleaned.contains("..")) {
            cleaned = cleaned.replaceAll("\\.{2,}", ".");
        }
        return cleaned;
    }

    public static String getExtension(String pathFrom, String PathTo) {
        String backendPathFrom = normalizePath(pathFrom);
        String backendPathTo = normalizePath(PathTo);
        String extension = "";
        int dotIndex = backendPathFrom.lastIndexOf('.');
        if (dotIndex != -1 || dotIndex != backendPathFrom.length() - 1) {
            extension = "." + backendPathFrom.substring(dotIndex + 1).toLowerCase();
            if (backendPathTo.toLowerCase().endsWith(extension)) {
                return "";
            }
        }
        return extension;
    }

    private static String addUserRootFolder(String rawPath, Long id) {
        return String.format("user-%s-files/%s", id, rawPath);
    }

    private static String addAppendixToPath(String backendPath, String appendix) {
        if (backendPath.isBlank() || appendix.isBlank()) {
            throw new BadPathFormatException("Undefined path for file or directory");
        }
        if (backendPath.endsWith("/")) {
            return String.format("%s%s", backendPath, appendix);
        }
        throw new BadPathFormatException("Parent path must be a directory (ends with /)");
    }

    private static String removeUserRootFolder(String backendPath) {
        return backendPath.replaceFirst("^[^/]+/", "");
    }

    //Не знаю насколько минус, но в этом методе очень важен порядок обработки, что делает его очень неподатливым к изменениям
    private static String removeTrailingSlash(String rawPath) {
        return rawPath.trim()
                .replace("\\", "/")
                .replaceAll("/{2,}", "/")
                .replaceAll("/\\s+/", "")
                .trim()
                .replaceAll("^/+", "")
                .replaceAll("\\s+/", "/");
    }

}