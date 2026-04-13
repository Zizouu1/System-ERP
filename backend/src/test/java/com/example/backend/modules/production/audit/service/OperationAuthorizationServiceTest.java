package com.example.backend.modules.production.audit.service;

import com.example.backend.modules.admin.usermanagement.entity.Role;
import com.example.backend.modules.admin.usermanagement.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class OperationAuthorizationServiceTest {

    private final OperationAuthorizationService service = new OperationAuthorizationService();

    @Test
    void adminCanUpdateAnyOperation() {
        User admin = User.builder()
                .username("admin")
                .role(Role.ADMIN)
                .matricule("A-1")
                .build();

        Assertions.assertDoesNotThrow(
                () -> service.assertCanUpdateOperation(admin, "someone", "X-1"));
    }

    @Test
    void ownerCanUpdateByUsername() {
        User user = User.builder()
                .username("john")
                .role(Role.PF)
                .matricule("PF-01")
                .build();

        Assertions.assertDoesNotThrow(
                () -> service.assertCanUpdateOperation(user, "john", "OTHER"));
    }

    @Test
    void nonOwnerCannotUpdate() {
        User user = User.builder()
                .username("john")
                .role(Role.PF)
                .matricule("PF-01")
                .build();

        Assertions.assertThrows(
                AccessDeniedException.class,
                () -> service.assertCanUpdateOperation(user, "anna", "PF-02"));
    }
}
