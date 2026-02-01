package com.example.backend.modules.admin.usermanagement.controller;

import com.example.backend.modules.admin.usermanagement.dto.RegisterRequest;
import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.admin.usermanagement.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService service;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register")
    public ResponseEntity<User> register(
            @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(service.register(request));
    }
}
