package com.example.backend.modules.admin.usermanagement.service;

import com.example.backend.modules.admin.usermanagement.dto.RegisterRequest;
import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.admin.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public User register(RegisterRequest request) {
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();
        return repository.save(user);
    }
}
