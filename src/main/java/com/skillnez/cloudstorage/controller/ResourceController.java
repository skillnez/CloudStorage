package com.skillnez.cloudstorage.controller;

import com.skillnez.cloudstorage.entity.CustomUserDetails;
import com.skillnez.cloudstorage.service.FileSystemService;
import com.skillnez.cloudstorage.utils.PathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ResourceController {

    private final FileSystemService fileSystemService;

    @Autowired
    public ResourceController(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
    }

    @PostMapping("/resource")
    public ResponseEntity<?> upload (@RequestParam("path") String path,
                                     @RequestParam("object") MultipartFile[] file,
                                     @AuthenticationPrincipal CustomUserDetails user) {
        String fullPath = PathFactory.addUserScopedPrefix(user.getId(), path);
        String fullNormalizedPath = PathFactory.normalizePath(fullPath);
        return ResponseEntity.status(201).body(fileSystemService.upload(fullNormalizedPath, file));
    }

}
