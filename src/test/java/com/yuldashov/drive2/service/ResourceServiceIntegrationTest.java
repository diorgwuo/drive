package com.yuldashov.drive2.service;


import com.yuldashov.drive2.ResponseResource;
import com.yuldashov.drive2.exceptions.ResourceNotFoundException;
import com.yuldashov.drive2.models.User;
import com.yuldashov.drive2.repository.UserRepository;
import com.yuldashov.drive2.support.AbstractIntegrationTest;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.util.List;

public class ResourceServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private MinioClient minioClient;
    @Autowired
    private UserService userService;
    @Autowired
    private PathService pathService;
    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private String userFolder;

    @BeforeEach
    public void setUser() throws MinioException {
        cleanMinio();
        cleanUsers();
        User user = new User();
        user.setUsername("testUser");
        user.setPassword("testuser");
        userService.register(user);
        testUser = userService.getUserByUsername("testUser");

        userFolder = "user-" + testUser.getId() + "-files";
    }

    private void cleanMinio() throws MinioException {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket("user-files")
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            String objectPath = result.get().objectName();
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("user-files")
                            .object(objectPath)
                            .build()
            );
        }
    }

    private void cleanUsers() {
        userRepository.deleteAll();
    }

    @Test
    public void fileUpload() throws MinioException, IOException {
        String path = "test1/";
        String content = "test1234";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                contentBytes
        );

        resourceService.uploadResource(userFolder, path, file);

        String objectPath = userFolder + "/" + path + "test.txt";

        StatObjectResponse result = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket("user-files")
                        .object(objectPath)
                        .build()
        );

        assertThat(result.size()).isEqualTo(contentBytes.length);
        assertThat(result.object()).isEqualTo(objectPath);
        assertThat(result.contentType()).isEqualTo("text/plain");
    }

    @Test
    public void renameResource() throws MinioException, IOException {
        String from = "test1/test.txt";
        String to = "test1/dior.txt";

        String path = "test1/";
        String content = "test1234";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                contentBytes
        );

        resourceService.uploadResource(userFolder, path, file);

        resourceService.moveResource(from, to, userFolder);

        assertThat(pathService.exists(userFolder + "/" + from)).isFalse();
        assertThat(pathService.exists(userFolder + "/" + to)).isTrue();
    }

    @Test
    public void deleteFile() throws MinioException, IOException {
        String path = "test1/test.txt";
        String fullPath = userFolder + "/" + path;

        String pathFile = "test1/";
        String content = "test1234";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                contentBytes
        );

        resourceService.uploadResource(userFolder, pathFile, file);

        assertThat(pathService.exists(fullPath)).isTrue();

        resourceService.deleteResource(path, userFolder);

        assertThat(pathService.exists(fullPath)).isFalse();

    }

    @Test
    public void deleteDirectory() throws MinioException, IOException {

        String parentPath = "test1/";
        resourceService.createDirectory(userFolder, parentPath);

        String path = "test1/test2/";
        String fullPath = userFolder + "/" + path;

        resourceService.createDirectory(userFolder, path);

        assertThat(pathService.exists(fullPath)).isTrue();

        String pathFile = "test1/test2/";
        String content = "test1234";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                contentBytes
        );

        resourceService.uploadResource(userFolder, path, file);

        assertThat(pathService.exists(userFolder + "/" + pathFile + "test.txt")).isTrue();

        resourceService.deleteResource(path, userFolder);

        assertThat(pathService.exists(fullPath)).isFalse();
        assertThat(pathService.exists(userFolder + "/" + pathFile + "test.txt")).isFalse();
    }

    @Test
    public void checkAccess() throws MinioException, IOException {
        User user2 = new User();
        user2.setUsername("testUser2");
        user2.setPassword("testuser");
        userService.register(user2);
        User testUser2 = userService.getUserByUsername("testUser2");
        String userFolder2 = "user-" + testUser2.getId() + "-files";

        String content = "test1234";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                contentBytes
        );

        resourceService.uploadResource(userFolder2, "", file);

        assertThat(pathService.exists(userFolder2 + "/test.txt")).isTrue();

        assertThatThrownBy(() -> resourceService.getResource(userFolder, "test.txt"))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(pathService.exists(userFolder2 + "/test.txt")).isTrue();

    }

    @Test
    public void searchUserFilesShouldCorrect() throws MinioException, IOException {
        User user2 = new User();
        user2.setUsername("testUser2");
        user2.setPassword("testuser");
        userService.register(user2);
        User testUser2 = userService.getUserByUsername("testUser2");
        String userFolder2 = "user-" + testUser2.getId() + "-files";

        String content = "dior1234";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "dior.txt",
                "text/plain",
                contentBytes
        );

        resourceService.uploadResource(userFolder2, "", file2);

        assertThat(pathService.exists(userFolder2 + "/")).isTrue();

        List<ResponseResource> responseResource = resourceService.searchResource("dior.txt", userFolder2);
        List<ResponseResource> responseResource2 = resourceService.searchResource("dior.txt", userFolder);

        assertThat(responseResource).isNotEmpty();
        assertThat(responseResource.get(0).getName()).isEqualTo("dior.txt");
        assertThat(responseResource2).isEmpty();
    }
}
