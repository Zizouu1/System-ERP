package com.example.backend.modules.admin.usermanagement.service;

import com.example.backend.modules.admin.usermanagement.dto.RegisterRequest;
import java.util.List;
import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.admin.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.backend.modules.admin.usermanagement.dto.UpdateUserRequest;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public User register(RegisterRequest request) {
        System.out.println("DEBUG: Service: registering " + request.getUsername() + " with role " + request.getRole());
        var user = User.builder()
                .Matricule(request.getMatricule())
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();
        User saved = repository.save(user);
        System.out.println("DEBUG: Service: saved user id " + saved.getId());
        return saved;
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

    public User getUserByMatricule(String Matricule) {
        return repository.findByMatricule(Matricule)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserByUsername(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
