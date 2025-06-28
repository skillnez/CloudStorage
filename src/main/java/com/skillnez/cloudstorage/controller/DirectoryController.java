package com.skillnez.cloudstorage.controller;

import com.skillnez.cloudstorage.dto.StorageInfoResponseDto;
import com.skillnez.cloudstorage.entity.CustomUserDetails;
import com.skillnez.cloudstorage.service.FileSystemService;
import com.skillnez.cloudstorage.utils.PathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DirectoryController {

    private final FileSystemService fileSystemService;
    @Autowired
    public DirectoryController(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
    }

    @GetMapping("/directory")
    public ResponseEntity<?> getStorageInfo(@AuthenticationPrincipal CustomUserDetails user, @RequestParam String path) {
        Long userId = user.getId();
        String backendPath = PathUtils.formatPathForBackend(path, userId);
        return ResponseEntity.ok(fileSystemService.getElementsInFolder(backendPath, userId));
    }

    @PostMapping("/directory")
    public ResponseEntity<StorageInfoResponseDto> createFolder(@AuthenticationPrincipal CustomUserDetails user,
                                                               @RequestParam String path) {
        String backendPath = PathUtils.formatPathForBackend(path, user.getId());
        return ResponseEntity.status(201).body(fileSystemService.createFolder(backendPath));
    }
}
