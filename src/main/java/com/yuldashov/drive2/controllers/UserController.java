package com.yuldashov.drive2.controllers;

import com.yuldashov.drive2.exceptions.UnautorizedException;
import com.yuldashov.drive2.models.User;
import com.yuldashov.drive2.service.AuthService;
import com.yuldashov.drive2.service.ResourceService;
import com.yuldashov.drive2.service.UserService;
import io.minio.errors.MinioException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.apache.tomcat.util.http.parser.Authorization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.*;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;
    private final AuthService authService;

    @Autowired
    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register (
            @Valid
            @RequestBody User user,
            HttpServletRequest request,
            HttpServletResponse response) throws MinioException {
        String rawPassword = user.getPassword();
        String username = userService.register(user);
        authService.autoLogin(user.getUsername(), rawPassword, response, request);
        return new ResponseEntity<>(username, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid
            @RequestBody User user,
            HttpServletRequest request,
            HttpServletResponse response) {
        String username = user.getUsername();
        String password = user.getPassword();
        authService.autoLogin(username, password, response, request);
        return ResponseEntity.ok(Map.of("username", username));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
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
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/get/me")
    public ResponseEntity<?> getUser(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnautorizedException("Вы не авторизованы");
        }
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        return new ResponseEntity<>(Map.of("username", user.getUsername()), HttpStatus.OK);
    }

}

