package com.skillnez.cloudstorage.controller;

import com.skillnez.cloudstorage.dto.ResourceType;
import com.skillnez.cloudstorage.dto.StorageInfoResponseDto;
import com.skillnez.cloudstorage.entity.CustomUserDetails;
import com.skillnez.cloudstorage.service.FileSystemService;
import com.skillnez.cloudstorage.service.PathService;
import com.skillnez.cloudstorage.utils.FolderTraversalMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DirectoryController {

    private final FileSystemService fileSystemService;
    private final PathService pathService;

    @Autowired
    public DirectoryController(FileSystemService fileSystemService, PathService pathService) {
        this.fileSystemService = fileSystemService;
        this.pathService = pathService;
    }

    @GetMapping("/directory")
    public ResponseEntity<?> getStorageInfo(@AuthenticationPrincipal CustomUserDetails user, @RequestParam String path) {
        Long userId = user.getId();
        String backendPath = pathService.formatPathForBackend(path, userId);
        return ResponseEntity.ok(fileSystemService.getElementsInFolder(backendPath, userId));
    }

    @PostMapping("/directory")
    public ResponseEntity<StorageInfoResponseDto> createFolder(@AuthenticationPrincipal CustomUserDetails user,
                                                               @RequestParam String path) {
        String backendPath = pathService.formatPathForBackend(path, user.getId());
        return ResponseEntity.status(201).body(fileSystemService.createFolder(backendPath));
    }
}
