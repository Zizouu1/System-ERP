package com.example.backend.modules.admin.usermanagement.service;

import com.example.backend.modules.admin.usermanagement.dto.RegisterRequest;
import com.example.backend.modules.admin.usermanagement.dto.UpdateUserRequest;
import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.admin.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public User register(RegisterRequest request) {
        var user = User.builder()
                .matricule(request.getMatricule())
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
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getMatricule() != null) {
            user.setMatricule(request.getMatricule());
        }
        if (request.getFirstname() != null) {
            user.setFirstname(request.getFirstname());
        }
        if (request.getLastname() != null) {
            user.setLastname(request.getLastname());
        }
        return repository.save(user);
    }

    public void deleteUser(Long id) {
        repository.deleteById(id);
    }

    public List<User> getAllUsers() {
        return repository.findAll();
    }
}
