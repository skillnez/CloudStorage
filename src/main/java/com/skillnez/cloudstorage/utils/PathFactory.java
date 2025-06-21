package com.skillnez.cloudstorage.utils;

import com.skillnez.cloudstorage.exception.BadPathFormatException;

public class PathFactory {

    private static final String EMPTY_STRING = "";

    public static String addUserScopedPrefix(String username, String normalizedPath) {
        return String.format("%s/%s", username, normalizedPath);
    }

    public static String getFileOrFolderName (String normalizedPath) {
        String[] pathPrefix = normalizedPath.split("/");
        return pathPrefix[pathPrefix.length - 1];
    }

    public static String getVisiblePath(String normalizedPath) {
        String cleaned = normalizedPath.replaceAll("/+$", "");
        int idx = cleaned.lastIndexOf('/');
        if (idx == -1) {
            return ""; // Корень
        } else {
            return cleaned.substring(0, idx + 1); // Включительно со слэшем
        }
    }

    public static String normalizeFolderPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new BadPathFormatException("path cant be empty");
        }
        if (rawPath.matches(".*[:*?\"<>|].*")) {
            throw new BadPathFormatException("path cannot contain ':  *  ?  \"  <  >  |'" );
        }
        String cleaned = rawPath.trim()
                .replace("\\", "/")
                .replaceAll("/{2,}", "/")
                .replaceAll("^/+", "");
        if (cleaned.isEmpty()) {
            throw new BadPathFormatException("path cant be empty");
        }
        if (cleaned.contains("..")) {
            throw new BadPathFormatException("path cannot contain .. in row");
        }
        return cleaned;
    }

}
