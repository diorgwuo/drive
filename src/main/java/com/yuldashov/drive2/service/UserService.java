package com.yuldashov.drive2.service;

import com.yuldashov.drive2.exceptions.UserAlreadyExistException;
import com.yuldashov.drive2.models.User;
import com.yuldashov.drive2.repository.UserRepository;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResourceService resourceService;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, ResourceService resourceService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.resourceService = resourceService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
    }

    @Transactional
    public String register(User user) throws MinioException {
        String username = user.getUsername();
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistException("Username уже занят");
        }
        String rawPassword = user.getPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        User savedUser = userRepository.save(user);
        resourceService.createUserBucket(savedUser.getId());
        return user.getUsername();
    }

    public User getUserByUsername(String username) {
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Не найден по имени"));
    }


}
