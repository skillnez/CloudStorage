package com.skillnez.cloudstorage.utils;

import com.skillnez.cloudstorage.exception.BadPathFormatException;

public class PathFactory {

    private static final String EMPTY_STRING = "";

    public static String addUserScopedPrefixForFolderCreation(String username, String normalizedPath) {
        return String.format("%s/%s/", username, normalizedPath);
    }

    public static String addUserScopedPrefixForFileUpload(String username, String normalizedPath) {
        return String.format("%s/%s", username, normalizedPath);
    }

    public static String getFileOrFolderName (String normalizedPath) {
        String[] pathPrefix = normalizedPath.split("/");
        return pathPrefix[pathPrefix.length - 1];
    }

    public static String getVisiblePath(String normalizedPath) {
        int index = normalizedPath.lastIndexOf("/");
        if (index == -1) {
            return EMPTY_STRING;
        }
        return normalizedPath.substring(0, index+1);
    }

    public static String normalizeFolderPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new BadPathFormatException("path cant be empty");
        }
        String cleaned = rawPath.trim()
                .replace("\\", "/")
                .replaceAll("/{2,}", "/")
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
        if (cleaned.isEmpty()) {
            throw new BadPathFormatException("path cant be empty");
        }
        if (cleaned.contains("..")) {
            throw new BadPathFormatException("path cannot contain .. in row");
        }
        return cleaned;
    }

}
