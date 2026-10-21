package com.yuldashov.drive2.service;

import com.yuldashov.drive2.exceptions.UserAlreadyExistException;
import com.yuldashov.drive2.models.User;
import com.yuldashov.drive2.repository.UserRepository;
import com.yuldashov.drive2.support.AbstractIntegrationTest;
import io.minio.errors.MinioException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

public class UserServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void saveUser() throws MinioException {
        User user = new User();
        user.setUsername("testUser");
        user.setPassword("test1234");


        String username = userService.register(user);

        Optional<User> savedUser = userRepository.findUserByUsername(username);

        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getUsername()).isEqualTo("testUser");
        assertThat(savedUser.get().getId()).isPositive();
        assertThat(savedUser.get().isAccountNonExpired()).isTrue();
        assertThat(savedUser.get().isEnabled()).isTrue();
        assertThat(savedUser.get().isCredentialsNonExpired()).isTrue();
        assertThat(savedUser.get().isAccountNonLocked()).isTrue();
    }

    @Test
    public void passwordShouldEncoded() throws MinioException {
        User user = new User();
        user.setUsername("testPassword");
        user.setPassword("password1256");

        userService.register(user);

        Optional<User> savedUser = userRepository.findUserByUsername("testPassword");

        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getPassword()).isNotEqualTo("password1256");

    }

    @Test
    public void usernameShouldBeUnique() throws MinioException {
        User user1 = new User();
        user1.setUsername("user1");
        user1.setPassword("user1235");
        userService.register(user1);

        User user2 = new User();
        user2.setUsername("user1");
        user2.setPassword("user2135");
        assertThatThrownBy(() -> userService.register(user2))
                .isInstanceOf(UserAlreadyExistException.class)
                .hasMessageContaining("Username уже занят");
    }

}

