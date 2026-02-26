package com.example.backend.modules.admin.usermanagement.dto;

import com.example.backend.modules.admin.usermanagement.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String Matricule;
    private String firstname;
    private String lastname;
    private String username;
    private String password;
    private Role role;
}
