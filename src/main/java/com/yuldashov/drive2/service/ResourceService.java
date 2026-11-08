package com.yuldashov.drive2.service;

import com.yuldashov.drive2.dto.ResponseResource;
import com.yuldashov.drive2.exceptions.ResourceAlreadyExistException;
import com.yuldashov.drive2.exceptions.ResourceNotFoundException;
import com.yuldashov.drive2.models.User;
import com.yuldashov.drive2.repository.MinioRepository;
import com.yuldashov.drive2.util.PathUtils;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ResourceService {
    private final MinioRepository minioRepository;
    private final static String BUCKET_NAME = "user-files";

    @PostConstruct
    public void createMainBucket() throws MinioException {
        if (!minioRepository.existBucket(BUCKET_NAME)) {
            minioRepository.createBucket(BUCKET_NAME);
        }
    }

    public ResponseResource getResource(User user, String path) throws MinioException {
        String fullPath = PathUtils.buildPathUserFolder(user.getId()) + "/" + path;

        if (!minioRepository.exists(fullPath)) {
            throw new ResourceNotFoundException("Ресурс не найден");
        }
        long size = minioRepository.getObject(fullPath).size();

        if (!path.endsWith("/")) {
            minioRepository.createDirectory(path);
            String name = PathUtils.getNameToFileOrDirectory(path);
            String responsePath = PathUtils.getPathResponseToFile(path);
            return new ResponseResource(responsePath, name, size);
        }
        String name = PathUtils.getNameToDirectory(path);
        String responsePath = PathUtils.getPathResponseToDirectory(path);
        return new ResponseResource(responsePath, name);
    }

    public void createUserBucket(int userId) throws MinioException {
        minioRepository.createDirectory(PathUtils.buildPathUserFolder(userId));
    }

    public ResponseResource createDirectory(User user, String path) throws MinioException {
        String userFolder = PathUtils.buildPathUserFolder(user.getId());
        String pathOnly = path.substring(0, path.length() - 1);
        int lastSlash = pathOnly.lastIndexOf("/");
        if (lastSlash >= 0) {
            String fullParentPath = userFolder + "/" + pathOnly.substring(0, lastSlash + 1);
            if (!minioRepository.exists(fullParentPath)) {
                throw new ResourceNotFoundException("Родительская папка не существует");
            }
        }

        String fullPath = userFolder + "/" + path;

        if (minioRepository.exists(fullPath)) {
            throw new ResourceAlreadyExistException("Папка уже существует");
        }

        minioRepository.createDirectory(fullPath);
        String nameDirectory = PathUtils.getNameToDirectory(path);
        String responsePath = PathUtils.getPathResponseToDirectory(path);
        return new ResponseResource(responsePath, nameDirectory);
    }

    public ResponseResource uploadResource(User user, String path,
                                           String filename, InputStream stream,
                                           Long size)
            throws MinioException {
        String userFolder = PathUtils.buildPathUserFolder(user.getId());
        String fullPath = userFolder + "/" + path + filename;

        if (minioRepository.exists(fullPath)) {
            throw new ResourceAlreadyExistException("Ресурс уже существует");
        }

        minioRepository.saveObject(fullPath, stream, size);
        String responsePath = PathUtils.getPathResponseToFile(fullPath);
        return new ResponseResource(responsePath, filename, size);
    }

    public List<ResponseResource> getDirectoryInfo(User user, String path) throws MinioException {
        String userFolder = PathUtils.buildPathUserFolder(user.getId());
        String fullPath = userFolder + "/" + path;
        if (!minioRepository.exists(fullPath)) {
            throw new ResourceNotFoundException("Папка не существует");
        }
        Iterable<Result<Item>> results = minioRepository.listDirectory(fullPath, false);
        return StreamSupport.stream(results.spliterator(), false)
                .map(result -> {
                    try {
                        return result.get();

                    } catch (MinioException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(item -> !item.objectName().equals(fullPath))
                .map(item -> {
                    String nameObject = item.objectName();
                    String pathOnly = nameObject.substring(userFolder.length() + 1);
                    if (nameObject.endsWith("/")) {
                        return new ResponseResource(
                                PathUtils.getPathResponseToDirectory(pathOnly),
                                PathUtils.getNameToDirectory(pathOnly));
                    }
                    return new ResponseResource(PathUtils.getPathResponseToFile(pathOnly),
                            PathUtils.getNameToFileOrDirectory(pathOnly));
                })
                .toList();
    }

    public List<ResponseResource> searchResource(String query, User user) throws MinioException {
        String userFolder = PathUtils.buildPathUserFolder(user.getId());
        List<ResponseResource> responseResources = new ArrayList<>();

        Iterable<Result<Item>> results = minioRepository.listDirectory(userFolder + "/", true);
        for (Result<Item> result : results) {
            Item item = result.get();
            String nameObject = item.objectName();
            String pathOnly = nameObject.substring(userFolder.length() + 1);

            if (pathOnly.contains(query)) {
                if (nameObject.endsWith("/")) {
                    String name = PathUtils.getNameToDirectory(pathOnly);
                    String responsePath = PathUtils.getPathResponseToDirectory(pathOnly);
                    responseResources.add(new ResponseResource(responsePath, name));
                } else {
                    String name = PathUtils.getNameToFileOrDirectory(pathOnly);
                    String responsePath = PathUtils.getPathResponseToFile(pathOnly);
                    responseResources.add(new ResponseResource(responsePath, name, item.size()));
                }
            }
        }
        return responseResources;
    }

    public ResponseResource moveResource(String from, String to, User user) throws MinioException {
        String userFolder = PathUtils.buildPathUserFolder(user.getId());
        String pathTo;
        pathTo = from.endsWith("/") && to.endsWith("/") ? PathUtils.getPathResponseToDirectory(to) : PathUtils.getPathResponseToFile(to);

        String fullFromPath = userFolder + "/" + from;
        String fullToPath = userFolder + "/" + to;

        if (!minioRepository.exists(fullFromPath)) {
            throw new ResourceNotFoundException("Ресурс не найден");
        }

        if (minioRepository.exists(fullToPath)) {
            throw new ResourceNotFoundException("Ресурс, лежащий по пути to уже существует");
        }

        if (!pathTo.isEmpty()) {
            minioRepository.createDirectory(userFolder + "/" + pathTo);
        }

        if (to.endsWith("/")) {
            moveDirectory(fullFromPath, fullToPath);
        } else {
            moveFile(fullFromPath, fullToPath);
        }

        StatObjectResponse response = minioRepository.getObject(fullToPath);
        if (to.endsWith("/")) {
            String name = PathUtils.getNameToDirectory(to);
            String responsePath = PathUtils.getPathResponseToDirectory(to);
            return new ResponseResource(responsePath, name);
        } else {
            String name = PathUtils.getNameToFileOrDirectory(to);
            String responsePath = PathUtils.getPathResponseToFile(to);
            return new ResponseResource(responsePath, name, response.size());
        }
    }

    private void moveDirectory(String from, String to) throws MinioException {
        List<String> objectNames = new ArrayList<>();
        Iterable<Result<Item>> results = minioRepository.listDirectory(from, true);

        for (Result<Item> result : results) {
            Item item = result.get();
            objectNames.add(item.objectName());
        }

        for (String oldObjectName : objectNames) {
            String objectName = oldObjectName.substring(from.length());
            String newObjectName = to + objectName;
            minioRepository.copyObject(newObjectName, oldObjectName);
        }

        for (String oldObjectName : objectNames) {
            minioRepository.removeObject(oldObjectName);
        }

        if (!minioRepository.exists(to)) {
            minioRepository.createDirectory(to);
        }
    }

    private void moveFile(String from, String to) throws MinioException {
        minioRepository.copyObject(to, from);
        minioRepository.removeObject(from);
    }

    public void deleteResource(String path, User user) throws MinioException {
        String userFolder = PathUtils.buildPathUserFolder(user.getId());
        String fullPath = userFolder + "/" + path;

        if (!minioRepository.exists(fullPath)) {
            throw new ResourceNotFoundException("Ресурс не найден");
        }

        if (path.endsWith("/")) {
            deleteDirectory(fullPath);
        } else {
            deleteFile(fullPath);
        }
    }

    private void deleteFile(String fullPath) throws MinioException {
        minioRepository.removeObject(fullPath);
    }

    private void deleteDirectory(String fullPath) throws MinioException {
        List<String> objectPaths = new ArrayList<>();
        Iterable<Result<Item>> results = minioRepository.listDirectory(fullPath, true);

        for (Result<Item> objectPath : results) {
            objectPaths.add(objectPath.get().objectName());
        }

        for (String object : objectPaths) {
            minioRepository.removeObject(object);
        }

        if (minioRepository.exists(fullPath)) {
            minioRepository.removeObject(fullPath);
        }
    }

    public void downloadResource(String path, User user, OutputStream stream, HttpServletResponse response) throws MinioException, IOException {
        String userFolder = PathUtils.buildPathUserFolder(user.getId());
        String fullPath = userFolder + "/" + path;

        if (!minioRepository.exists(fullPath)) {
            throw new ResourceNotFoundException("Ресурс не найден");
        }

        if (path.endsWith("/")) {
            downloadDirectory(fullPath, stream);
        } else {
            downloadFile(fullPath, stream);
        }
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + PathUtils.getNameForZip(path) + "\"");
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    private void downloadFile(String fullPath, OutputStream stream) throws MinioException, IOException {
        try (GetObjectResponse response = minioRepository.getByteObject(fullPath)) {
            response.transferTo(stream);
        }
    }

    private void downloadDirectory(String fullPath, OutputStream stream) throws IOException, MinioException {
        ZipOutputStream zout = new ZipOutputStream(stream);
        Iterable<Result<Item>> results = minioRepository.listDirectory(fullPath, true);
        for (Result<Item> result : results) {
            Item item = result.get();
            String objectPath = item.objectName();
            String resourceName = objectPath.substring(fullPath.length());
            if (objectPath.endsWith("/")) {
                zout.putNextEntry(new ZipEntry(resourceName));
                zout.closeEntry();
            } else {
                zout.putNextEntry(new ZipEntry(resourceName));
                try (GetObjectResponse response = minioRepository.getByteObject(objectPath)) {
                    response.transferTo(zout);
                }
                zout.closeEntry();
            }
        }
        zout.finish();
    }
}
