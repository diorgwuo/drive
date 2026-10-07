package com.yuldashov.drive2.service;

import com.yuldashov.drive2.ResponseResource;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class PathService {

    private final MinioClient minioClient;

    public PathService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public String getNameToFile(String path) {
        int slash = path.lastIndexOf("/");
        if (slash >= 0) {
            String name = path.substring(slash + 1);
            return name;
        }
        return path;
    }

    public String getPathResponseToFile(String path) {
        int slash = path.lastIndexOf("/");
        if (slash >= 0) {
            String responsePath = path.substring(0, slash+1);
            return responsePath;
        }
        return "";
    }

    public String getNameToDirectory(String path) {
        String p = path.replaceAll("/$", "");
        int slash = p.lastIndexOf("/");
        if (slash >= 0) {
            String name = p.substring(slash+1);
            return name;
        }
        return p;
    }

    public String getPathResponseToDirectory(String path) {
        String p = path.replaceAll("/$", "");
        int slash = p.lastIndexOf("/");
        if (slash >= 0) {
            String responsePath = p.substring(0, slash+1);
            return responsePath;
        }
        return "";
    }

    public void createDirectories(String path) throws MinioException {
        String p = path.replaceAll("/$","");

        String[] directories = p.split("/");

        StringBuilder pathDir = new StringBuilder();

        for (int i = 0; i<directories.length; i++) {
            if (i>0) {
                pathDir.append("/");
            }
            pathDir.append(directories[i]);
            String fullPath = pathDir.toString() + "/";

            if (!exists(fullPath)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket("user-files")
                                .object(fullPath)
                                .stream(new ByteArrayInputStream(new byte[] {}), 0L, -1L)
                                .build()
                );
            }
        }
    }

    public boolean exists(String fullPath) throws MinioException {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket("user-files")
                            .object(fullPath)
                            .build()
            );
            return true;

        } catch (ErrorResponseException e) {
            return false;
        }
    }
}
