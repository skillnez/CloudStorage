package com.skillnez.cloudstorage.controller;

import com.skillnez.cloudstorage.dto.StorageInfoResponseDto;
import com.skillnez.cloudstorage.entity.CustomUserDetails;
import com.skillnez.cloudstorage.service.FileSystemService;
import com.skillnez.cloudstorage.utils.PathUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ResourceController {

    private final FileSystemService fileSystemService;

    @Autowired
    public ResourceController(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
    }

    @GetMapping("/resource")
    public ResponseEntity<StorageInfoResponseDto> getResource(@RequestParam("path") String path,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        String backendPath = PathUtils.formatPathForBackend(path, user.getId());
        StorageInfoResponseDto resource = fileSystemService.getElement(backendPath, user.getId());
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/resource/search")
    public ResponseEntity<List<StorageInfoResponseDto>> search(@AuthenticationPrincipal CustomUserDetails user,
                                    @RequestParam("query") String query) {
        String backendPath = PathUtils.formatPathForBackend("", user.getId());
        List<StorageInfoResponseDto> searchResult = fileSystemService.searchElements(backendPath, query, user.getId());
        return ResponseEntity.ok(searchResult);
    }

    @GetMapping("/resource/download")
    public void download(@RequestParam("path") String path,
                                      @AuthenticationPrincipal CustomUserDetails user,
                                      HttpServletResponse response) throws IOException {
        String backendPath = PathUtils.formatPathForBackend(path, user.getId());
        if (backendPath.endsWith("/")) {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=archive.zip");
            fileSystemService.downloadFolder(backendPath, user.getId(), response.getOutputStream());
            ResponseEntity.ok().build();
        } else {
            try (InputStream downloadStream = fileSystemService.downloadFile(backendPath, user.getId())) {
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + Paths.get(backendPath).getFileName() + "\"");
                IOUtils.copy(downloadStream, response.getOutputStream());
                response.flushBuffer();
            }
        }
    }

    @GetMapping("/resource/move")
    public ResponseEntity<StorageInfoResponseDto> move(@RequestParam("from") String pathFrom,
                                  @RequestParam("to") String pathTo,
                                  @AuthenticationPrincipal CustomUserDetails user){
        String backendPathFrom = PathUtils.formatPathForBackend(pathFrom, user.getId());
        String backendPathTo = PathUtils.formatPathForBackend(pathTo, user.getId());
        return ResponseEntity.ok().body(fileSystemService.moveOrRename(backendPathFrom, backendPathTo, user.getId()));
    }


    @PostMapping("/resource")
    public ResponseEntity<List<StorageInfoResponseDto>> upload(@RequestParam("path") String path,
                                    @RequestParam("object") MultipartFile[] file,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        String backendPath = PathUtils.formatPathForBackend(path, user.getId());
        return ResponseEntity.status(201).body(fileSystemService.upload(backendPath, file));
    }

    @DeleteMapping("/resource")
    public ResponseEntity<String> delete(@RequestParam("path") String path,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        String backendPath = PathUtils.formatPathForBackend(path, user.getId());
        fileSystemService.delete(backendPath, user.getId());
        return ResponseEntity.status(204).build();
    }

}
