package com.skillnez.cloudstorage.utils;

import com.skillnez.cloudstorage.exception.BadPathFormatException;

public class PathFactory {

    private static final String EMPTY_STRING = "";

    public static String addUserScopedPrefix(Long id, String normalizedPath) {
        return String.format("user-%s-files/%s", id, normalizedPath);
    }

    public static String removeUserScopedPrefix(String username, String fullNormalizedPath) {
        return fullNormalizedPath.replaceFirst(username+"/", EMPTY_STRING);
    }

    public static String getFileOrFolderName (String normalizedPath) {
        String[] pathPrefix = normalizedPath.split("/");
        if (normalizedPath.endsWith("/")) {
            return pathPrefix[pathPrefix.length - 1] + "/";
        }
        return pathPrefix[pathPrefix.length - 1];
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
