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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ResourceController {

    private final FileSystemService fileSystemService;

    @Autowired
    public ResourceController(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
    }

    @GetMapping("/resource")
    public ResponseEntity<?> getResource(@AuthenticationPrincipal CustomUserDetails user) {
        List<StorageInfoResponseDto> searchResult = new ArrayList<>();
        searchResult.add(new StorageInfoResponseDto("user-8-files/", "", null, ResourceType.DIRECTORY));
        return ResponseEntity.ok(searchResult);
    }

    @GetMapping("/resource/search")
    public ResponseEntity<?> search (@AuthenticationPrincipal CustomUserDetails user,
                                     @RequestParam("query") String path) {
        String fullSearchPath = PathFactory.addUserScopedPrefix(user.getId(), path);
        String fullNormalizedSearchPath = PathFactory.normalizePath(fullSearchPath);
        List<StorageInfoResponseDto> searchResult = fileSystemService.getElementsInFolder(fullNormalizedSearchPath).
                stream().filter(dto -> dto.getPath()
                        .toLowerCase()
                        .contains(fullSearchPath))
                .collect(Collectors.toList());

        return ResponseEntity.ok(searchResult);
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
