package com.example.backend.modules.admin.employee.service;

import com.example.backend.modules.admin.employee.dto.EmployeeLookupResponse;
import com.example.backend.modules.admin.employee.dto.EmployeeRequest;
import com.example.backend.modules.admin.employee.dto.EmployeeResponse;
import com.example.backend.modules.admin.employee.entity.Employee;
import com.example.backend.modules.admin.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private static final int MATRICULE_MAX_LENGTH = 50;
    private static final int TEXT_MAX_LENGTH = 120;
    private static final int LOOKUP_LIMIT = 20;

    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listAll() {
        return employeeRepository.findAllByOrderByMatriculeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return employeeRepository.count();
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        ValidatedEmployee payload = validate(request);

        if (employeeRepository.existsByMatriculeIgnoreCase(payload.matricule())) {
            throw new IllegalArgumentException("Un employe avec ce matricule existe deja.");
        }

        Employee saved = employeeRepository.save(Employee.builder()
                .matricule(payload.matricule())
                .nom(payload.nom())
                .prenom(payload.prenom())
                .poste(payload.poste())
                .departement(payload.departement())
                .build());

        return toResponse(saved);
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employe introuvable."));

        ValidatedEmployee payload = validate(request);

        if (employeeRepository.existsByMatriculeIgnoreCaseAndIdNot(payload.matricule(), id)) {
            throw new IllegalArgumentException("Un employe avec ce matricule existe deja.");
        }

        existing.setMatricule(payload.matricule());
        existing.setNom(payload.nom());
        existing.setPrenom(payload.prenom());
        existing.setPoste(payload.poste());
        existing.setDepartement(payload.departement());

        return toResponse(employeeRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new IllegalArgumentException("Employe introuvable.");
        }
        employeeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<EmployeeLookupResponse> lookup(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return employeeRepository.searchForLookup(normalizedQuery, PageRequest.of(0, LOOKUP_LIMIT))
                .stream()
                .map(this::toLookupResponse)
                .toList();
    }

    @Transactional
    public String importCsv(List<String[]> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide.");
        }

        int success = 0;
        int failed = 0;
        StringBuilder errors = new StringBuilder();

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row == null || row.length < 5) {
                failed++;
                errors.append("Ligne ")
                        .append(i + 1)
                        .append(": colonnes insuffisantes (matricule, nom, prenom, poste, departement).\n");
                continue;
            }

            try {
                EmployeeRequest request = EmployeeRequest.builder()
                        .matricule(row[0])
                        .nom(row[1])
                        .prenom(row[2])
                        .poste(row[3])
                        .departement(row[4])
                        .build();

                ValidatedEmployee payload = validate(request);

                Employee employee = employeeRepository.findByMatriculeIgnoreCase(payload.matricule())
                        .orElseGet(Employee::new);

                employee.setMatricule(payload.matricule());
                employee.setNom(payload.nom());
                employee.setPrenom(payload.prenom());
                employee.setPoste(payload.poste());
                employee.setDepartement(payload.departement());

                employeeRepository.save(employee);
                success++;
            } catch (Exception e) {
                failed++;
                errors.append("Ligne ")
                        .append(i + 1)
                        .append(": ")
                        .append(e.getMessage())
                        .append("\n");
            }
        }

        return String.format(
                "Succes: %d employes importes/mis a jour. Echecs: %d.%n%s",
                success,
                failed,
                errors);
    }

    private ValidatedEmployee validate(EmployeeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La charge utile est obligatoire.");
        }

        String matricule = requireText(request.getMatricule(), "matricule", MATRICULE_MAX_LENGTH);
        String nom = requireText(request.getNom(), "nom", TEXT_MAX_LENGTH);
        String prenom = requireText(request.getPrenom(), "prenom", TEXT_MAX_LENGTH);
        String poste = requireText(request.getPoste(), "poste", TEXT_MAX_LENGTH);
        String departement = requireText(request.getDepartement(), "departement", TEXT_MAX_LENGTH);

        return new ValidatedEmployee(matricule, nom, prenom, poste, departement);
    }

    private String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le champ '" + fieldName + "' est obligatoire.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    "Le champ '" + fieldName + "' depasse la taille maximale de " + maxLength + " caracteres.");
        }
        return normalized;
    }

    private EmployeeResponse toResponse(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .matricule(employee.getMatricule())
                .nom(employee.getNom())
                .prenom(employee.getPrenom())
                .poste(employee.getPoste())
                .departement(employee.getDepartement())
                .build();
    }

    private EmployeeLookupResponse toLookupResponse(Employee employee) {
        return EmployeeLookupResponse.builder()
                .matricule(employee.getMatricule())
                .nom(employee.getNom())
                .prenom(employee.getPrenom())
                .poste(employee.getPoste())
                .departement(employee.getDepartement())
                .build();
    }

    private record ValidatedEmployee(
            String matricule,
            String nom,
            String prenom,
            String poste,
            String departement) {
    }
}
