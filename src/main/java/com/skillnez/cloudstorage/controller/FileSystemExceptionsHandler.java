package com.skillnez.cloudstorage.controller;

import com.skillnez.cloudstorage.exception.BadPathFormatException;
import com.skillnez.cloudstorage.exception.FolderAlreadyExistsException;
import com.skillnez.cloudstorage.exception.NoParentFolderException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {DirectoryController.class, ResourceController.class})
public class FileSystemExceptionsHandler {

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<?> handleNoHandlerFoundException(NoHandlerFoundException e) {
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


}
