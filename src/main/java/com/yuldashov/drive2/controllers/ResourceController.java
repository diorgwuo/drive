package com.yuldashov.drive2.controllers;

import com.yuldashov.drive2.dto.ResponseResource;
import com.yuldashov.drive2.models.User;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ResourceController {
    private final ResourceService resourceService;

    @GetMapping("/resource")
    @ResponseStatus(HttpStatus.OK)
    public ResponseResource getResource(
            @RequestParam("path")
            @NotBlank(message = "Путь невалидный или отсутствует")
            String path,
            @AuthenticationPrincipal User user
    ) throws MinioException {
        return resourceService.getResource(user, path);
    }

    @PostMapping(value = "/directory")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseResource createDirectory(
            @RequestParam("path")
            @NotBlank(message = "Путь невалидный или отсутствует")
            String path,
            @AuthenticationPrincipal User user) throws MinioException {
        return resourceService.createDirectory(user, path);
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
            @AuthenticationPrincipal User user) throws MinioException, IOException {
        return resourceService.uploadResource(user, path, filename,
                request.getInputStream(), request.getContentLengthLong());
    }

    @GetMapping("/directory")
    @ResponseStatus(HttpStatus.OK)
    public List<ResponseResource> getDirectoryInfo(
            @RequestParam("path")
            @NotBlank(message = "Путь невалидный или отсутствует")
            String path,
            @AuthenticationPrincipal User user) throws MinioException {
        return resourceService.getDirectoryInfo(user, path);
    }

    @GetMapping("/resource/search")
    @ResponseStatus(HttpStatus.OK)
    public List<ResponseResource> searchResource(
            @RequestParam("query")
            @NotBlank(message = "Путь невалидный или отсутствует")
            String query,
            @AuthenticationPrincipal User user) throws MinioException {
        return resourceService.searchResource(query, user);
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
            @AuthenticationPrincipal User user) throws MinioException {
        return resourceService.moveResource(from, to, user);
    }

    @DeleteMapping("/resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(
            @RequestParam("path")
            @NotBlank(message = "Путь невалидный или отсутствует")
            String path,
            @AuthenticationPrincipal User user) throws MinioException {
        resourceService.deleteResource(path, user);
    }

    @GetMapping("/resource/download")
    @ResponseStatus(HttpStatus.OK)
    public void downloadResource(
            @RequestParam("path")
            @NotBlank(message = "Путь невалидный или отсутствует")
            String path,
            HttpServletResponse response,
            @AuthenticationPrincipal User user) throws MinioException, IOException {
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + PathUtils.getNameForZip(path) + "\"");
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        resourceService.downloadResource(path, user, response.getOutputStream());
    }
}
