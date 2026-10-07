package com.yuldashov.drive2.controllers;

import com.yuldashov.drive2.ResponseResource;
import com.yuldashov.drive2.exceptions.InvalidPathException;
import com.yuldashov.drive2.exceptions.InvalideFileException;
import com.yuldashov.drive2.exceptions.UnautorizedException;
import com.yuldashov.drive2.models.User;
import com.yuldashov.drive2.service.ResourceService;
import com.yuldashov.drive2.service.UserService;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

@RestController
public class ResourceController {
    private final ResourceService resourceService;
    private final UserService userService;

    @Autowired
    public ResourceController(ResourceService resourceService, UserService userService) {
        this.resourceService = resourceService;
        this.userService = userService;
    }

    @GetMapping("/resource")
    public ResponseEntity<?> getResource(
            @RequestParam("path") String path,
            Authentication authentication
    ) throws MinioException {

        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnautorizedException("Вы не авторизованы");
        }

        if (path == null || path.trim().isEmpty()) {
            throw new InvalidPathException("Путь невалидный или отсутствует");
        }

        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        String userFolder = "user-" + user.getId() + "-files";

        ResponseResource responseResource = resourceService.getResource(userFolder, path);
        return new ResponseEntity<>(responseResource, HttpStatus.OK);
    }

    @PostMapping(value = "/directory",
    produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createDirectory(
            @RequestParam("path") String path,
            Authentication authentication) throws MinioException {

        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnautorizedException("Вы не авторизованы");
        }

        if (path == null || path.trim().isEmpty()) {
            throw new InvalidPathException("Путь невалидный или отсутствует");
        }

        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        String userFolder = "user-" + user.getId() + "-files";
        ResponseResource responseResource = resourceService.createDirectory(userFolder, path);
        return new ResponseEntity<>(responseResource, HttpStatus.CREATED);
    }

    @PostMapping("/resource")
    public ResponseEntity<?> uploudResource(
            @RequestParam("path") String path,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws MinioException, IOException {

        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnautorizedException("Вы не авторизованы");
        }

        if (path == null || path.trim().isEmpty()) {
            throw new InvalidPathException("Путь невалидный или отсутствует");
        }

        if (file == null || file.isEmpty()) {
            throw new InvalideFileException("Невалидное тело запроса");
        }

        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        String userFolder = "user-" + user.getId() + "-files";

        ResponseResource responseResource = resourceService.uploadResource(userFolder, path, file);
        return new ResponseEntity<>(responseResource, HttpStatus.CREATED);
    }

    @GetMapping("/directory")
    public ResponseEntity<?> getDirectoryInfo(
            @RequestParam("path") String path,
            Authentication authentication) throws MinioException {

        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnautorizedException("Вы не авторизованы");
        }

        if (path == null || path.trim().isEmpty()) {
            throw new InvalidPathException("Путь невалидный или отсутствует");
        }

        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        String userFolder = "user-" + user.getId() + "-files";
        List<ResponseResource> responseResources = resourceService.getDirectoryInfo(userFolder,path);
        return ResponseEntity.ok(responseResources);
    }

    @GetMapping("/resource/search")
    public ResponseEntity<?> searchResource(
            @RequestParam("query") String query,
            Authentication authentication) throws MinioException {

        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnautorizedException("Вы не авторизованы");
        }

        if (query == null || query.trim().isEmpty()) {
            throw new InvalidPathException("Путь невалидный или отсутствует");
        }

        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        String userFolder = "user-" + user.getId() + "-files";

        List<ResponseResource> responseResources = resourceService.searchResource(query, userFolder);

        return ResponseEntity.ok(responseResources);
    }

    @GetMapping("/resource/move")
    public ResponseEntity<?> moveResource(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            Authentication authentication) throws MinioException {

        if (to == null || to.trim().isEmpty() || from == null || from.trim().isEmpty()) {
            throw new InvalidPathException("Путь невалидный или отсутствует");
        }

        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnautorizedException("Вы не авторизованы");
        }

        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        String userFolder = "user-" + user.getId() + "-files";

        ResponseResource responseResource = resourceService.moveResource(from, to, userFolder);

        return ResponseEntity.ok(responseResource);
    }

    @DeleteMapping("/resource")
    public ResponseEntity<?> deleteResource(
            @RequestParam("path") String path,
            Authentication authentication) throws MinioException {

        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnautorizedException("Вы не авторизованы");
        }

        if (path == null || path.trim().isEmpty()) {
            throw new InvalidPathException("Путь невалидный или отсутствует");
        }

        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        String userFolder = "user-" + user.getId() + "-files";



        resourceService.deleteResource(path, userFolder);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/resource/download")
    public ResponseEntity<?> downloadResource(
        @RequestParam("path") String path,
        Authentication authentication) throws MinioException, IOException {

        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnautorizedException("Вы не авторизованы");
        }

        if (path == null || path.trim().isEmpty()) {
            throw new InvalidPathException("Путь невалидный или отсутствует");
        }

        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        String userFolder = "user-" + user.getId() + "-files";

        byte[] data = resourceService.downloadResource(path,userFolder);
        String fileName;

        if (path.endsWith("/")) {
            String folderName = path.replaceAll("/$", "");
            folderName = folderName.substring(folderName.lastIndexOf("/")+1);
            fileName = folderName + ".zip";
        } else {
            fileName = path.substring(path.lastIndexOf("/")+1);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
