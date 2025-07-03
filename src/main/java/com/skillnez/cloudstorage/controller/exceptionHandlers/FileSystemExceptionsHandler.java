package com.skillnez.cloudstorage.controller.exceptionHandlers;

import com.skillnez.cloudstorage.controller.DirectoryController;
import com.skillnez.cloudstorage.controller.ResourceController;
import com.skillnez.cloudstorage.exception.BadPathFormatException;
import com.skillnez.cloudstorage.exception.FolderAlreadyExistsException;
import com.skillnez.cloudstorage.exception.NoParentFolderException;
import com.skillnez.cloudstorage.exception.UploadErrorException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice(assignableTypes = {DirectoryController.class, ResourceController.class})
public class FileSystemExceptionsHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNoSuchElementException(NoSuchElementException e) {
        return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(BadPathFormatException.class)
    public ResponseEntity<?> handleBadPathFormatException(BadPathFormatException e) {
        return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(FolderAlreadyExistsException.class)
    public ResponseEntity<?> handleFolderAlreadyExistsException(FolderAlreadyExistsException e) {
        return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(NoParentFolderException.class)
    public ResponseEntity<?> handleNoParentFolderException(NoParentFolderException e) {
        return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(UploadErrorException.class)
    public ResponseEntity<?> handleUploadErrorException(UploadErrorException e) {
        return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
    }


}
