package com.yuldashov.drive2.controllers;

import com.yuldashov.drive2.dto.ResponseResource;
import com.yuldashov.drive2.service.ResourceService;
import com.yuldashov.drive2.util.PathUtils;
import io.minio.errors.MinioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
public class ResourceController {
    private final ResourceService resourceService;

    @GetMapping("/resource")
    @ResponseStatus(HttpStatus.OK)
    public ResponseResource getResource(
            @RequestParam("path")
            @NotBlank(message = "Путь невалидный или отсутствует")
            String path,
            Authentication auth
    ) throws MinioException {
        return resourceService.getResource(auth, path);
    }

    @PostMapping(value = "/directory")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseResource createDirectory(
            @RequestParam("path")
            @NotBlank(message = "Путь невалидный или отсутствует")
            String path,
            Authentication auth) throws MinioException {
        return resourceService.createDirectory(auth, path);
    }

    @PostMapping(value = "/resource", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseResource uploadResource(
            @RequestParam("path")
            @NotBlank(message = "Путь невалидный или отсутствует")
            String path,
            @RequestParam("filename")
            @NotBlank(message = "Название файла не может быть пустым")
            String filename,
            HttpServletRequest request,
            Authentication auth) throws MinioException, IOException {
        return resourceService.uploadResource(auth, path, filename,
                request.getInputStream(), request.getContentLengthLong());
    }

    @GetMapping("/directory")
    @ResponseStatus(HttpStatus.OK)
    public List<ResponseResource> getDirectoryInfo(
            @RequestParam("path")
            @NotBlank(message = "Путь невалидный или отсутствует")
            String path,
            Authentication auth) throws MinioException {
        return resourceService.getDirectoryInfo(auth, path);
    }

    @GetMapping("/resource/search")
    @ResponseStatus(HttpStatus.OK)
    public List<ResponseResource> searchResource(
            @RequestParam("query")
            @NotBlank(message = "Путь невалидный или отсутствует")
            String query,
            Authentication auth) throws MinioException {
        return resourceService.searchResource(query, auth);
    }

    @GetMapping("/resource/move")
    @ResponseStatus(HttpStatus.OK)
    public ResponseResource moveResource(
            @RequestParam("from")
            @NotBlank(message = "Путь невалидный или отсутствует")
            String from,
            @RequestParam("to")
            @NotBlank(message = "Путь невалидный или отсутствует")
            String to,
            Authentication auth) throws MinioException {
        return resourceService.moveResource(from, to, auth);
    }

    @DeleteMapping("/resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(
            @RequestParam("path")
            @NotBlank(message = "Путь невалидный или отсутствует")
            String path,
            Authentication auth) throws MinioException {
        resourceService.deleteResource(path, auth);
    }

    @GetMapping("/resource/download")
    @ResponseStatus(HttpStatus.OK)
    public void downloadResource(
            @RequestParam("path")
            @NotBlank(message = "Путь невалидный или отсутствует")
            String path,
            HttpServletResponse response,
            Authentication auth) throws MinioException, IOException {
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + PathUtils.getNameForZip(path) + "\"");
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        resourceService.downloadResource(path, auth, response.getOutputStream());
    }
}
