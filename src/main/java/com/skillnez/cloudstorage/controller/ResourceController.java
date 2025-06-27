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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ResourceController {

    private final FileSystemService fileSystemService;
    private final PathService pathService;

    @Autowired
    public ResourceController(FileSystemService fileSystemService, PathService pathService) {
        this.fileSystemService = fileSystemService;
        this.pathService = pathService;
    }

//    @GetMapping("/resource")
//    public ResponseEntity<?> getResource(@AuthenticationPrincipal CustomUserDetails user) {
//        List<StorageInfoResponseDto> searchResult = new ArrayList<>();
//        searchResult.add(new StorageInfoResponseDto("user-8-files/", "", null, ResourceType.DIRECTORY));
//        return ResponseEntity.ok(searchResult);
//    }
//
    @GetMapping("/resource/search")
    public ResponseEntity<?> search (@AuthenticationPrincipal CustomUserDetails user,
                                     @RequestParam("query") String query) {
        String backendPath = pathService.formatPathForBackend("", user.getId());
        List<StorageInfoResponseDto> searchResult = fileSystemService.searchElements(backendPath, query, user.getId());
//todo надо добавить проверку чтобы если нет родительской папки создаваемого ресурса, то создавать его
        return ResponseEntity.ok(searchResult);
    }


    @PostMapping("/resource")
    public ResponseEntity<?> upload (@RequestParam("path") String path,
                                     @RequestParam("object") MultipartFile[] file,
                                     @AuthenticationPrincipal CustomUserDetails user) {
        String backendPath = pathService.formatPathForBackend(path, user.getId());
        return ResponseEntity.status(201).body(fileSystemService.upload(backendPath, file));
    }

}
