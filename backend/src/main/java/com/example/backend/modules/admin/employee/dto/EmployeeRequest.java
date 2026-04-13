package com.example.backend.modules.admin.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequest {
    private String matricule;
    private String nom;
    private String prenom;
    private String poste;
    private String departement;
}
