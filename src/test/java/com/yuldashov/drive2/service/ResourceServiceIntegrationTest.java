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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ResourceServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ResourceService resourceService;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private UserService userService;
    @Autowired
    private MinioRepository minioRepository;
    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private String userFolder;

    @BeforeEach
    public void setUser() throws MinioException {
        cleanMinio();
        cleanUsers();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        userService.register("testUser", "testuser", response, request);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                testUser, null);
        testUser = userService.validate(auth);

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
        Authentication auth = new UsernamePasswordAuthenticationToken(
                testUser, null);
        String path = "test1/";
        String content = "test1234";
        String filename = "test.txt";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream(contentBytes);

        resourceService.uploadResource(auth, path, filename, stream, (long) contentBytes.length);

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
        Authentication auth = new UsernamePasswordAuthenticationToken(
                testUser, null);
        String from = "test1/test.txt";
        String to = "test1/dior.txt";

        String path = "test1/";
        String content = "test1234";
        String filename = "test.txt";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream(contentBytes);

        resourceService.uploadResource(auth, path, filename, stream, (long) contentBytes.length);
        resourceService.moveResource(from, to, auth);

        assertThat(minioRepository.exists(userFolder + "/" + from)).isFalse();
        assertThat(minioRepository.exists(userFolder + "/" + to)).isTrue();
    }

    @Test
    public void deleteFile() throws MinioException, IOException {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                testUser, null);
        String path = "test1/test.txt";
        String fullPath = userFolder + "/" + path;

        String content = "test1234";
        String filename = "test.txt";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream(contentBytes);

        resourceService.uploadResource(auth, path, filename, stream, (long) contentBytes.length);

        assertThat(minioRepository.exists(fullPath)).isTrue();

        resourceService.deleteResource(path, auth);

        assertThat(minioRepository.exists(fullPath)).isFalse();

    }

    @Test
    public void deleteDirectory() throws MinioException, IOException {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                testUser, null
        );

        String parentPath = "test1/";
        resourceService.createDirectory(auth, parentPath);

        String path = "test1/test2/";
        String fullPath = userFolder + "/" + path;

        resourceService.createDirectory(auth, path);

        assertThat(minioRepository.exists(fullPath)).isTrue();

        String pathFile = "test1/test2/";
        String content = "test1234";
        String filename = "test.txt";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream(contentBytes);

        resourceService.uploadResource(auth, path, filename, stream, (long) contentBytes.length);

        assertThat(minioRepository.exists(userFolder + "/" + pathFile + "test.txt")).isTrue();

        resourceService.deleteResource(path, auth);

        assertThat(minioRepository.exists(fullPath)).isFalse();
        assertThat(minioRepository.exists(userFolder + "/" + pathFile + "test.txt")).isFalse();
    }

    @Test
    public void checkAccess() throws MinioException, IOException {
        Authentication auth = new UsernamePasswordAuthenticationToken(
               testUser, null);
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        userService.register("testUser2", "testuser", response2, request2);
        User testUser2 = userService.validate(auth);
        Authentication auth2 = new UsernamePasswordAuthenticationToken(
                testUser2, null
        );
        String userFolder2 = "user-" + testUser2.getId() + "-files";

        String content = "test1234";
        String filename = "test.txt";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream(contentBytes);

        resourceService.uploadResource(auth, "", filename, stream, (long) contentBytes.length);

        assertThat(minioRepository.exists(userFolder2 + "/test.txt")).isTrue();

        assertThatThrownBy(() -> resourceService.getResource(auth, "test.txt"))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(minioRepository.exists(userFolder2 + "/test.txt")).isTrue();

    }

    @Test
    public void searchUserFilesShouldCorrect() throws MinioException, IOException {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                testUser, null
        );
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        userService.register("testUser2", "testuser" , response2, request2);
        User testUser2 = userService.validate(auth);
        Authentication auth2 = new UsernamePasswordAuthenticationToken(
                testUser2, null
        );
        String userFolder2 = "user-" + testUser2.getId() + "-files";

        String content = "dior1234";
        String filename = "test.txt";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream(contentBytes);

        resourceService.uploadResource(auth, "", filename, stream, (long) contentBytes.length);

        assertThat(minioRepository.exists(userFolder2 + "/")).isTrue();

        List<ResponseResource> responseResource = resourceService.searchResource("dior.txt", auth2);
        List<ResponseResource> responseResource2 = resourceService.searchResource("dior.txt", auth);

        assertThat(responseResource).isNotEmpty();
        assertThat(responseResource.get(0).getName()).isEqualTo("dior.txt");
        assertThat(responseResource2).isEmpty();
    }
}
