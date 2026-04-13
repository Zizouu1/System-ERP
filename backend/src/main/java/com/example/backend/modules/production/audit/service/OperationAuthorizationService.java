package com.example.backend.modules.production.audit.service;

import com.example.backend.modules.admin.usermanagement.entity.Role;
import com.example.backend.modules.admin.usermanagement.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class OperationAuthorizationService {

    public boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    public void assertCanUpdateOperation(User actor, String createdByUsername, String operatorMatricule) {
        if (isAdmin(actor)) {
            return;
        }
        if (actor == null) {
            throw new AccessDeniedException("Utilisateur non authentifié.");
        }

        boolean ownsByUsername = createdByUsername != null
                && createdByUsername.equalsIgnoreCase(actor.getUsername());
        boolean ownsByMatricule = operatorMatricule != null
                && actor.getMatricule() != null
                && operatorMatricule.equalsIgnoreCase(actor.getMatricule());

        if (!ownsByUsername && !ownsByMatricule) {
            throw new AccessDeniedException("Vous ne pouvez modifier que vos propres opérations.");
        }
    }

    public void assertAdmin(User actor) {
        if (!isAdmin(actor)) {
            throw new AccessDeniedException("Seul l'administrateur peut effectuer cette action.");
        }
    }
}
