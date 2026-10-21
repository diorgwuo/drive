package com.yuldashov.drive2.controllers;

import com.yuldashov.drive2.exceptions.UnautorizedException;
import com.yuldashov.drive2.models.User;
import com.yuldashov.drive2.service.AuthService;
import com.yuldashov.drive2.service.UserService;
import io.minio.errors.MinioException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public String register (
            @Valid
            @RequestBody User user,
            HttpServletRequest request,
            HttpServletResponse response) throws MinioException {
        String rawPassword = user.getPassword();
        String username = userService.register(user);
        authService.autoLogin(user.getUsername(), rawPassword, response, request);
        return username;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public String login(
            @Valid
            @RequestBody User user,
            HttpServletRequest request,
            HttpServletResponse response) {
        String username = user.getUsername();
        String password = user.getPassword();
        authService.autoLogin(username, password, response, request);
        return username;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnautorizedException("Вы не авторизованы");
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        Cookie cookie = new Cookie("SESSION", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    @GetMapping("/get/me")
    @ResponseStatus(HttpStatus.OK)
    public String getUser(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnautorizedException("Вы не авторизованы");
        }
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        return user.getUsername();
    }
}

