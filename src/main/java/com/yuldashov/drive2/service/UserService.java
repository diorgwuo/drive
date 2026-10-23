package com.yuldashov.drive2.service;

import com.yuldashov.drive2.exceptions.InvalidCredentialsException;
import com.yuldashov.drive2.exceptions.UnautorizedException;
import com.yuldashov.drive2.exceptions.UserAlreadyExistException;
import com.yuldashov.drive2.models.User;
import com.yuldashov.drive2.repository.UserRepository;
import io.minio.errors.MinioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final ResourceService resourceService;
    private final SecurityContextRepository securityContextRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
    }

    @Transactional
    public void register(String username, String rawPassword, HttpServletResponse response,
                         HttpServletRequest request) throws MinioException {
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistException("Username уже занят");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        User savedUser = userRepository.save(user);
        resourceService.createUserBucket(savedUser.getId());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                savedUser, null, savedUser.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        securityContextRepository.saveContext(context, request, response);
    }

    public void login(String username, String rawPassword,
                      HttpServletResponse response,
                      HttpServletRequest request) {
        try {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, rawPassword);
            Authentication authentication = authenticationManager.authenticate(token);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(context, request, response);
        } catch (BadCredentialsException exception) {
            throw new InvalidCredentialsException("Неверные данные");
        }
    }

    public User validate(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new UnautorizedException("Вы не авторизованы");
        }
        return user;
    }
}
