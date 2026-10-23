package com.yuldashov.drive2.repository;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

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

    public void putObject(String path) throws MinioException {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(path)
                        .stream(new ByteArrayInputStream(new byte[]{}), 0L, -1L)
                        .build()
        );
    }

    public void saveObject(String path, InputStream stream, Long size) throws MinioException {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(path)
                            .stream(stream, size, -1L)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioException("Ошибка загрузки файла", e);
        }
    }

    public boolean exists(String path) throws MinioException {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(path)
                            .build()
            );
            return true;

        } catch (ErrorResponseException e) {
            return false;
        }
    }

    public StatObjectResponse getObject(String path) throws MinioException {
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(path)
                        .build()
        );
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

    public void copyObject(String newObjectName, String oldObjectName) throws MinioException {
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
    }

    public void removeObject(String path) throws MinioException {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(path)
                        .build()
        );
    }

    public GetObjectResponse getByteObject(String path) throws MinioException {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(path)
                        .build());
    }
}
