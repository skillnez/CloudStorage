package com.skillnez.cloudstorage.controller;

import com.skillnez.cloudstorage.exception.MinioOperationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class AllControllersExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(400).body(Map.of("message", "request parameters are invalid or contains unsupported characters"));
    }

    @ExceptionHandler(MinioOperationException.class)
    public ResponseEntity<?> handleMinioOperationException(MinioOperationException e) {
        return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
    }

}
