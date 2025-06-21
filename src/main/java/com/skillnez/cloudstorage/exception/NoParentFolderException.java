package com.skillnez.cloudstorage.exception;

public class NoParentFolderException extends RuntimeException {
    public NoParentFolderException(String message) {
        super(message);
    }
}
