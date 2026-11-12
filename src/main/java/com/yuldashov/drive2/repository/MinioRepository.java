package com.yuldashov.drive2.repository;

import com.yuldashov.drive2.exceptions.MinioStorageException;
import com.yuldashov.drive2.exceptions.ResourceNotFoundException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MinioRepository {
    private final MinioClient minioClient;
    private static final String BUCKET_NAME = "user-files";

    public boolean existBucket(String bucketName) throws MinioException {
        return minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build());
    }

    public void createBucket(String bucketName) throws MinioException {
        minioClient.makeBucket(
                MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build()
        );
    }

    public void createDirectory(String path) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(path)
                            .stream(new ByteArrayInputStream(new byte[]{}), 0L, -1L)
                            .build()
            );
        } catch (MinioException e) {
            log.error("Ошибка Minio", e);
            throw new MinioStorageException("Не удалось создать папку");
        }
    }

    public void saveObject(String path, InputStream stream, Long size) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(path)
                            .stream(stream, size, -1L)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                            .build()
            );
        } catch (MinioException e) {
            log.error("Ошибка Minio", e);
            throw new MinioStorageException("Ошибка загрузки файла");
        }
    }

    public boolean exists(String path) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(path)
                            .build()
            );
            return true;

        } catch (MinioException e) {
            if (e instanceof ErrorResponseException error &&
                    "NoSuchKey".equals(error.errorResponse().code())) {
                return false;
            }
            log.error("Ошибка Minio", e);
            throw new MinioStorageException("Не удалось проверить существует ли ресурс");
        }
    }

    public StatObjectResponse getObject(String path) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(path)
                            .build()
            );
        } catch (MinioException e) {
            if (e instanceof ErrorResponseException error &&
                    "NoSuchKey".equals(error.errorResponse().code())) {
                throw new ResourceNotFoundException("Ресурс не найден");
            }
            log.error("Ошибка Minio", e);
            throw new MinioStorageException("Не удалось получить ресурс");
        }
    }

    public Iterable<Result<Item>> listDirectory(String path, boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(BUCKET_NAME)
                        .recursive(recursive)
                        .prefix(path)
                        .build()
        );
    }

    public void copyObject(String newObjectName, String oldObjectName) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(newObjectName)
                            .source(SourceObject.builder()
                                    .bucket(BUCKET_NAME)
                                    .object(oldObjectName)
                                    .build())
                            .build()
            );
        } catch (MinioException e) {
            log.error("Ошибка Minio", e);
            throw new MinioStorageException("Ошибка Minio");
        }
    }

    public void removeObject(String path) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(path)
                            .build()
            );
        } catch (MinioException e) {
            log.error("Ошибка Minio", e);
            throw new MinioStorageException("Ошибка Minio");
        }
    }

    public GetObjectResponse getByteObject(String path) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(path)
                            .build());
        } catch (MinioException e) {
            log.error("Ошибка Minio", e);
            throw new MinioStorageException("Ошибка Minio");
        }
    }
}
