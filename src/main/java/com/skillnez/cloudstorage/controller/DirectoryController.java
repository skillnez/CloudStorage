package com.skillnez.cloudstorage.controller;

import com.skillnez.cloudstorage.dto.ResourceType;
import com.skillnez.cloudstorage.dto.StorageInfoResponseDto;
import com.skillnez.cloudstorage.entity.CustomUserDetails;
import com.skillnez.cloudstorage.service.FileSystemService;
import com.skillnez.cloudstorage.utils.PathFactory;
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
        String fullPath = PathFactory.addUserScopedPrefix(user.getId(), path);
        String fullNormalizedPath = PathFactory.normalizePath(fullPath);
        fileSystemService.checkFolderExists(fullNormalizedPath);
        return ResponseEntity.ok(fileSystemService.getElementsInFolder(fullNormalizedPath));
    }

    @PostMapping("/directory")
    public ResponseEntity<?> createFolder(@AuthenticationPrincipal CustomUserDetails user, @RequestParam String path) {
        String normalizedPath = PathFactory.normalizePath(path);
        String fullNormalizedPath = PathFactory.addUserScopedPrefix(user.getId(), normalizedPath);
        fileSystemService.checkFolderAlreadyExists(fullNormalizedPath);
        fileSystemService.checkParentFolders(fullNormalizedPath);
        fileSystemService.createFolder(fullNormalizedPath);
        return ResponseEntity.ok(new StorageInfoResponseDto
                (fullNormalizedPath, PathFactory.getFileOrFolderName(normalizedPath), ResourceType.DIRECTORY));
    }
}
