package com.yuldashov.drive2.service;

import com.yuldashov.drive2.exceptions.UserAlreadyExistException;
import com.yuldashov.drive2.models.User;
import com.yuldashov.drive2.repository.UserRepository;
import com.yuldashov.drive2.support.AbstractIntegrationTest;
import io.minio.errors.MinioException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

public class UserServiceIntegrationTest extends AbstractIntegrationTest {
    private static final String USERNAME = "testUser";
    private static final String PASSWORD = "test1234";
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void saveUser() throws MinioException {
        User user = new User();
        user.setUsername(USERNAME);
        user.setPassword(PASSWORD);
        userService.register(USERNAME, PASSWORD, response, request);
        Optional<User> savedUser = userRepository.findUserByUsername(user.getUsername());
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getUsername()).isEqualTo(USERNAME);
        assertThat(savedUser.get().getId()).isPositive();
        assertThat(savedUser.get().isAccountNonExpired()).isTrue();
        assertThat(savedUser.get().isEnabled()).isTrue();
        assertThat(savedUser.get().isCredentialsNonExpired()).isTrue();
        assertThat(savedUser.get().isAccountNonLocked()).isTrue();
    }

    @Test
    public void passwordShouldEncoded() throws MinioException {
        userService.register("testPassword", "password1256", response, request);
        Optional<User> savedUser = userRepository.findUserByUsername("testPassword");

        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getPassword()).isNotEqualTo("password1256");
    }

    @Test
    public void usernameShouldBeUnique() throws MinioException {
        userService.register("user1", "user1235", response, request);
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        assertThatThrownBy(() -> userService.register("user1", "user2135", response2, request2))
                .isInstanceOf(UserAlreadyExistException.class)
                .hasMessageContaining("Username уже занят");
    }
}

