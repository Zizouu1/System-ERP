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
                .matricule(request.getMatricule())
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

    public User getUserByMatricule(String matricule) {
        return repository.findByMatricule(matricule)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    public User getUserByUsername(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }
}
