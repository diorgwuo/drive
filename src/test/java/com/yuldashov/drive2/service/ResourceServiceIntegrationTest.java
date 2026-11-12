package com.yuldashov.drive2.service;

import com.yuldashov.drive2.dto.ResponseResource;
import com.yuldashov.drive2.exceptions.ResourceNotFoundException;
import com.yuldashov.drive2.models.User;
import com.yuldashov.drive2.repository.MinioRepository;
import com.yuldashov.drive2.repository.UserRepository;
import com.yuldashov.drive2.support.AbstractIntegrationTest;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequiredArgsConstructor
public class ResourceServiceIntegrationTest extends AbstractIntegrationTest {
    private static final String USERNAME = "testUser";
    private static final String PASSWORD = "testuser";

    private ResourceService resourceService;
    private MinioClient minioClient;
    private UserService userService;
    private MinioRepository minioRepository;
    private UserRepository userRepository;
    private CustomUserDetailsService userDetailsService;

    private User testUser;
    private String userFolder;

    @BeforeEach
    public void setUser() throws MinioException {
        cleanMinio();
        cleanUsers();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        userService.register(USERNAME, PASSWORD, response, request);
        testUser = new User();
        testUser.setUsername(USERNAME);
        testUser.setPassword(PASSWORD);

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
        String filename = "test.txt";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream(contentBytes);

        resourceService.uploadResource(testUser, path, filename, stream, (long) contentBytes.length);

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
        String filename = "test.txt";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream(contentBytes);

        resourceService.uploadResource(testUser, path, filename, stream, (long) contentBytes.length);
        resourceService.moveResource(from, to, testUser);

        assertThat(minioRepository.exists(userFolder + "/" + from)).isFalse();
        assertThat(minioRepository.exists(userFolder + "/" + to)).isTrue();
    }

    @Test
    public void deleteFile() throws MinioException, IOException {
        String path = "test1/test.txt";
        String fullPath = userFolder + "/" + path;

        String content = "test1234";
        String filename = "test.txt";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream(contentBytes);

        resourceService.uploadResource(testUser, path, filename, stream, (long) contentBytes.length);

        assertThat(minioRepository.exists(fullPath)).isTrue();

        resourceService.deleteResource(path, testUser);

        assertThat(minioRepository.exists(fullPath)).isFalse();

    }

    @Test
    public void deleteDirectory() throws MinioException, IOException {
        String parentPath = "test1/";
        resourceService.createDirectory(testUser, parentPath);

        String path = "test1/test2/";
        String fullPath = userFolder + "/" + path;

        resourceService.createDirectory(testUser, path);

        assertThat(minioRepository.exists(fullPath)).isTrue();

        String pathFile = "test1/test2/";
        String content = "test1234";
        String filename = "test.txt";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream(contentBytes);

        resourceService.uploadResource(testUser, path, filename, stream, (long) contentBytes.length);

        assertThat(minioRepository.exists(userFolder + "/" + pathFile + "test.txt")).isTrue();

        resourceService.deleteResource(path, testUser);

        assertThat(minioRepository.exists(fullPath)).isFalse();
        assertThat(minioRepository.exists(userFolder + "/" + pathFile + "test.txt")).isFalse();
    }

    @Test
    public void checkAccess() throws MinioException, IOException {
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        User testUser2 = new User();
        testUser2.setUsername("testUser2");
        testUser2.setPassword("testuser");
        userService.register("testUser2", "testuser", response2, request2);
        String userFolder2 = "user-" + testUser2.getId() + "-files";

        String content = "test1234";
        String filename = "test.txt";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream(contentBytes);

        resourceService.uploadResource(testUser, "", filename, stream, (long) contentBytes.length);

        assertThat(minioRepository.exists(userFolder2 + "/test.txt")).isTrue();

        assertThatThrownBy(() -> resourceService.getResource(testUser, "test.txt"))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(minioRepository.exists(userFolder2 + "/test.txt")).isTrue();

    }

    @Test
    public void searchUserFilesShouldCorrect() throws MinioException, IOException {
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        userService.register("testUser2", "testuser", response2, request2);
        User testUser2 = new User();
        testUser2.setUsername("testUser2");
        testUser2.setPassword("testuser");
        String userFolder2 = "user-" + testUser2.getId() + "-files";

        String content = "dior1234";
        String filename = "test.txt";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream(contentBytes);

        resourceService.uploadResource(testUser, "", filename, stream, (long) contentBytes.length);

        assertThat(minioRepository.exists(userFolder2 + "/")).isTrue();

        List<ResponseResource> responseResource = resourceService.searchResource("dior.txt", testUser2);
        List<ResponseResource> responseResource2 = resourceService.searchResource("dior.txt", testUser);

        assertThat(responseResource).isNotEmpty();
        assertThat(responseResource.get(0).getName()).isEqualTo("dior.txt");
        assertThat(responseResource2).isEmpty();
    }
}
