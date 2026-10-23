package com.yuldashov.drive2.controllers;

import com.yuldashov.drive2.dto.UserResponse;
import com.yuldashov.drive2.exceptions.UnautorizedException;
import com.yuldashov.drive2.models.User;
import com.yuldashov.drive2.service.AuthService;
import com.yuldashov.drive2.service.UserService;
import io.minio.errors.MinioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register (
            @Valid
            @RequestBody User user,
            HttpServletRequest request,
            HttpServletResponse response) throws MinioException {
        userService.register(user.getUsername(), user.getPassword(), response, request);
        return new UserResponse(user.getUsername());
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse login(
            @Valid
            @RequestBody User user,
            HttpServletRequest request,
            HttpServletResponse response) {
        userService.login(user.getUsername(), user.getPassword(), response, request);
        return new UserResponse(user.getUsername());
    }

    @GetMapping("/get/me")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse getUser(Authentication auth) {
        return new UserResponse(userService.validate(auth).getUsername());
    }
}

