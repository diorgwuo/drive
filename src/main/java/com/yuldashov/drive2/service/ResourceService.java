package com.yuldashov.drive2.service;

import com.yuldashov.drive2.ResponseResource;
import com.yuldashov.drive2.exceptions.ResourceAlreadyExistException;
import com.yuldashov.drive2.exceptions.ResourceNotFoundException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ResourceService {
    private final MinioClient minioClient;
    private final PathService pathService;

    public ResourceService(MinioClient minioClient, PathService pathService) {
        this.minioClient = minioClient;
        this.pathService = pathService;
    }

    @PostConstruct
    public void createMainBucket() throws MinioException {
        boolean existBucket = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket("user-files").build()
        );
        if (!existBucket) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket("user-files").build()
            );
        }

    }


    public ResponseResource getResource(String userFolder, String path) throws MinioException {
        String fullPath = userFolder + "/" + path;

        if (!pathService.exists(fullPath)) {
            throw new ResourceNotFoundException("Ресурс не найден");
        }

        StatObjectResponse objectStat =
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket("user-files")
                                .object(fullPath)
                                .build()
                );

        Long size = objectStat.size();

        if (!path.endsWith("/")) {
            String name = pathService.getNameToFile(path);
            String responsePath = pathService.getPathResponseToFile(path);
            return new ResponseResource(responsePath, name, size);
        }
        String name = pathService.getNameToDirectory(path);
        String responsePath = pathService.getPathResponseToDirectory(path);
        return new ResponseResource(responsePath, name);
    }

    public void createUserBucket(int userId) throws MinioException {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("user-files")
                        .object("user-" + userId + "-files/")
                        .stream(new ByteArrayInputStream(new byte[]{}), 0L, -1L)
                        .build()
        );
    }

    public ResponseResource createDirectory(String userFolder, String path) throws MinioException {

        String pathOnly = path.replaceAll("/$", "");
        int lastSlash = pathOnly.lastIndexOf("/");
        if (lastSlash >= 0) {
            String fullParentPath = userFolder + "/" + pathOnly.substring(0, lastSlash + 1);
            if (!pathService.exists(fullParentPath)) {
                throw new ResourceNotFoundException("Родительская папка не существует");
            }
        }

        String fullPath = userFolder + "/" + path;

        if (pathService.exists(fullPath)) {
            throw new ResourceAlreadyExistException("Папка уже существует");
        }

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("user-files")
                        .object(fullPath)
                        .stream(new ByteArrayInputStream(new byte[]{}), 0L, -1L)
                        .build()
        );
        String nameDirectory = pathService.getNameToDirectory(path);
        String responsePath = pathService.getPathResponseToDirectory(path);
        ResponseResource responseResource = new ResponseResource(responsePath, nameDirectory);
        return responseResource;
    }

    public ResponseResource uploadResource(String userFolder, String path, MultipartFile file) throws MinioException, IOException {

        String nameFile = file.getOriginalFilename();
        int lastSlash = nameFile.lastIndexOf('/');
        String filePath = "";
        String fileName;
        if (lastSlash >= 0) {
            filePath = nameFile.substring(0, lastSlash + 1);
            fileName = nameFile.substring(lastSlash + 1);
        } else {
            fileName = nameFile;
        }

        String fullPath = userFolder + "/" + path + nameFile;

        if (pathService.exists(fullPath)) {
            throw new ResourceAlreadyExistException("Ресурс уже существует");
        }

        pathService.createDirectories(userFolder + "/" + path + filePath);

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("user-files")
                        .object(fullPath)
                        .stream(file.getInputStream(), file.getSize(), -1L)
                        .contentType(file.getContentType())
                        .build()
        );

        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket("user-files")
                        .object(fullPath)
                        .build()
        );

        String responsePath = path + filePath;
        return new ResponseResource(responsePath, fileName, file.getSize());
    }

    public List<ResponseResource> getDirectoryInfo(String userFolder, String path) throws MinioException {
        String fullPath = userFolder + "/" + path;

        if (!pathService.exists(fullPath)) {
            throw new ResourceNotFoundException("Папка не существует");
        }

        List<ResponseResource> responseResources = new ArrayList<>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket("user-files")
                        .recursive(false)
                        .prefix(fullPath)
                        .build()
        );
        System.out.println("fullPath = [" + fullPath + "]");

        for (Result<Item> result : results) {
            Item item = result.get();
            String nameObject = item.objectName();
            System.out.println("📄 Найден объект: [" + nameObject + "]");
            System.out.println("📄 nameObject.length = " + nameObject.length());
            System.out.println("📄 байты: " + java.util.Arrays.toString(nameObject.getBytes()));

            if (nameObject.equals(fullPath)) {
                continue;
            }

            String pathOnly = nameObject.substring(userFolder.length() + 1);

            String name;
            String responsePath;

            if (nameObject.endsWith("/")) {
                name = pathService.getNameToDirectory(pathOnly);
                responsePath = pathService.getPathResponseToDirectory(pathOnly);
                responseResources.add(new ResponseResource(responsePath, name));
            } else {
                name = pathService.getNameToFile(pathOnly);
                responsePath = pathService.getPathResponseToFile(pathOnly);
                responseResources.add(new ResponseResource(responsePath, name, item.size()));
            }

        }
        return responseResources;
    }

    public List<ResponseResource> searchResource(String query, String userFolder) throws MinioException {
        List<ResponseResource> responseResources = new ArrayList<>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket("user-files")
                        .prefix(userFolder + "/")
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            String nameObject = item.objectName();

            String pathOnly = nameObject.substring(userFolder.length() + 1);
            String name;
            String responsePath;

            if (pathOnly.toLowerCase().contains(query.toLowerCase())) {
                if (nameObject.endsWith("/")) {
                    name = pathService.getNameToDirectory(pathOnly);
                    responsePath = pathService.getPathResponseToDirectory(pathOnly);
                    responseResources.add(new ResponseResource(responsePath, name));
                } else {
                    name = pathService.getNameToFile(pathOnly);
                    responsePath = pathService.getPathResponseToFile(pathOnly);
                    responseResources.add(new ResponseResource(responsePath, name, item.size()));
                }
            }
        }
        return responseResources;
    }

    public ResponseResource moveResource(String from, String to, String userFolder) throws MinioException {
        String pathTo;
        if (from.endsWith("/") && to.endsWith("/")) {
            pathTo = pathService.getPathResponseToDirectory(to);
        } else {
            pathTo = pathService.getPathResponseToFile(to);
        }

        String fullFromPath = userFolder + "/" + from;
        String fullToPath = userFolder + "/" + to;

        if (!pathService.exists(fullFromPath)) {
            throw new ResourceNotFoundException("Ресурс не найден");
        }

        if (pathService.exists(fullToPath)) {
            throw new ResourceNotFoundException("Ресурс, лежащий по пути to уже существует");
        }

        if (!pathTo.isEmpty()) {
            pathService.createDirectories(userFolder + "/" + pathTo);
        }

        if (to.endsWith("/")) {
            moveDirectory(fullFromPath, fullToPath);
        } else {
            moveFile(fullFromPath, fullToPath);
        }

        StatObjectResponse response = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket("user-files")
                        .object(fullToPath)
                        .build()
        );

        if (to.endsWith("/")) {
            String name = pathService.getNameToDirectory(to);
            String responsePath = pathService.getPathResponseToDirectory(to);
            return new ResponseResource(responsePath, name);
        } else {
            String name = pathService.getNameToFile(to);
            String responsePath = pathService.getPathResponseToFile(to);
            return new ResponseResource(responsePath, name, response.size());
        }


    }

    public void moveDirectory(String from, String to) throws MinioException {
        List<String> objectNames = new ArrayList<>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket("user-files")
                        .prefix(from)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            objectNames.add(item.objectName());
        }

        for (String oldObjectName : objectNames) {
            String objectName = oldObjectName.substring(from.length());
            String newObjectName = to + objectName;

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket("user-files")
                            .object(newObjectName)
                            .source(SourceObject.builder()
                                    .bucket("user-files")
                                    .object(oldObjectName)
                                    .build())
                            .build()
            );
        }

        for (String oldObjectName : objectNames) {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("user-files")
                            .object(oldObjectName)
                            .build()
            );
        }

        if (!pathService.exists(to)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket("user-files")
                            .object(to)
                            .stream(new ByteArrayInputStream(new byte[]{}), 0L, -1L)
                            .build()
            );
        }

    }

    public void moveFile(String from, String to) throws MinioException {
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket("user-files")
                        .object(to)
                        .source(SourceObject.builder()
                                .bucket("user-files")
                                .object(from)
                                .build())
                        .build());

        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket("user-files")
                        .object(from)
                        .build()
        );
    }

    public void deleteResource(String path, String userFolder) throws MinioException {
        String fullPath = userFolder + "/" + path;

        if (!pathService.exists(fullPath)) {
            throw new ResourceNotFoundException("Ресурс не найден");
        }

        if (path.endsWith("/")) {
            deleteDirectory(fullPath);
        } else {
            deleteFile(fullPath);
        }

    }

    public void deleteFile(String fullPath) throws MinioException {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket("user-files")
                        .object(fullPath)
                        .build()
        );

    }

    public void deleteDirectory(String fullPath) throws MinioException {
        List<String> objectPaths = new ArrayList<>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket("user-files")
                        .prefix(fullPath)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> objectPath : results) {
            objectPaths.add(objectPath.get().objectName());
        }

        for (String object : objectPaths) {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("user-files")
                            .object(object)
                            .build()
            );
        }

        if (pathService.exists(fullPath)) {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("user-files")
                            .object(fullPath)
                            .build()
            );
        }
    }

    public byte[] downloadResource(String path, String userFolder) throws MinioException, IOException {
        String fullPath = userFolder + "/" + path;

        if (!pathService.exists(fullPath)) {
            throw new ResourceNotFoundException("Ресурс не найден");
        }

        if (path.endsWith("/")) {
            return downloadDirectory(fullPath);
        } else {
            return downloadFile(fullPath);
        }
    }

    public byte[] downloadFile(String fullPath) throws MinioException, IOException {
        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("user-files")
                        .object(fullPath)
                        .build())) {

            byte[] data = response.readAllBytes();
            return data;
        }
    }

    public byte[] downloadDirectory(String fullPath) throws IOException, MinioException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(baos);

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket("user-files")
                        .prefix(fullPath)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            String objectPath = item.objectName();

            if (objectPath.endsWith("/")) {
                continue;
            }

            String resourceName = objectPath.substring(fullPath.length());

            try (GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket("user-files")
                            .object(objectPath)
                            .build())) {

                ZipEntry entry = new ZipEntry(resourceName);
                zout.putNextEntry(entry);

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = response.read(buffer)) != -1) {
                    zout.write(buffer, 0, bytesRead);
                }
                zout.closeEntry();
            }
        }
        zout.close();
        byte[] zip = baos.toByteArray();
        return zip;
    }
}
