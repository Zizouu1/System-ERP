package com.example.backend.modules.admin.usermanagement.service;

import com.example.backend.modules.admin.usermanagement.dto.RegisterRequest;
import java.util.List;
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

    public User updateUser(Long id, UpdateUserRequest request) {
        var user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        return repository.save(user);
    }

    public void deleteUser(Long id) {
        repository.deleteById(id);
    }

    public List<User> getAllUsers() {
        return repository.findAll();
    }

    public User getUserByUsername(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
