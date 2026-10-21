package com.yuldashov.drive2.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name="users")
public class User implements UserDetails {
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Setter
    @NotBlank(message = "Username не может быть пустым")
    @Size(min = 3, max = 20, message = "Username состоит от 3 до 20 символов")
    @Column(nullable = false, unique = true)
    private String username;
    @Setter
    @NotBlank(message = "Password не может быть пустым")
    @Size(min = 5, message = "Password состоит от 5 до 20 символов")
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    private boolean enabled = true;

    public User() {}

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
