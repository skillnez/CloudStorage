package com.skillnez.cloudstorage.controller;

import com.skillnez.cloudstorage.dto.ResourceType;
import com.skillnez.cloudstorage.dto.StorageInfoResponseDto;
import com.skillnez.cloudstorage.service.FileSystemService;
import com.skillnez.cloudstorage.utils.PathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DirectoryController {

    @Value("${minio.bucket-name}")
    private String bucketName;

    private final FileSystemService fileSystemService;

    @Autowired
    public DirectoryController(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
    }

    @PostMapping("/directory")
    public ResponseEntity<?> createFolder(@AuthenticationPrincipal UserDetails user, @RequestParam String path) {
        String normalizedPath = PathFactory.normalizeFolderPath(path);
        String visiblePath = PathFactory.getVisiblePath(normalizedPath);
        String fullNormalizedPath = PathFactory.addUserScopedPrefixForFolderCreation(user.getUsername(), normalizedPath);
        fileSystemService.checkParentFolders(fullNormalizedPath);
        fileSystemService.createFolder(fullNormalizedPath);
        return ResponseEntity.ok(new StorageInfoResponseDto
                (visiblePath, PathFactory.getFileOrFolderName(normalizedPath), ResourceType.DIRECTORY));
    }
}
