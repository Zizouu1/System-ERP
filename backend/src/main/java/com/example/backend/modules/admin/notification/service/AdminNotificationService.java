package com.example.backend.modules.admin.notification.service;

import com.example.backend.modules.admin.notification.dto.AdminNotificationResponse;
import com.example.backend.modules.admin.notification.entity.AdminNotification;
import com.example.backend.modules.admin.notification.repository.AdminNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {

    private final AdminNotificationRepository repository;

    public List<AdminNotificationResponse> listNotifications() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public long unreadCount() {
        long unread = repository.countUnread();
        long seen = repository.countBySeenTrue();
        log.debug("[notif] unreadCount queried -> unread={}, seen={}", unread, seen);
        return unread;
    }

    @Transactional
    public AdminNotificationResponse markSeen(Long id) {
        Long notificationId = Objects.requireNonNull(id, "id must not be null");

        if (notificationId <= 0) {
            throw new IllegalArgumentException("ID notification invalide.");
        }

        log.info("[notif] markSeen request -> id={}", notificationId);

        int updated = repository.markSeenById(notificationId);
        log.info("[notif] markSeen query result -> id={}, rowsAffected={}", notificationId, updated);

        AdminNotification notif = repository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable."));
        log.info("[notif] markSeen post-update fetch -> id={}, seen={}", notificationId, notif.isSeen());

        if (updated == 0) {
            if (!notif.isSeen()) {
                notif.setSeen(true);
                repository.saveAndFlush(notif);
                notif = repository.findById(notificationId)
                        .orElseThrow(() -> new IllegalArgumentException("Notification introuvable."));
                log.warn("[notif] markSeen fallback saveAndFlush applied -> id={}, seen={}", notificationId, notif.isSeen());
            }
        }

        log.info("[notif] markSeen completed -> id={}, seenAfter={}", notificationId, notif.isSeen());

        return toResponse(notif);
    }

    @Transactional
    public long markAllSeen() {
        long unreadBefore = repository.countUnread();
    List<Long> sampleUnreadIdsBefore = repository.findTop5BySeenFalseOrderByCreatedAtDesc()
                .stream()
                .map(AdminNotification::getId)
                .toList();

        log.info("[notif] markAllSeen request -> unreadBefore={}, sampleUnreadIds={}", unreadBefore, sampleUnreadIdsBefore);

        int updated = repository.markAllUnseenAsSeen();
        long unreadAfter = repository.countUnread();

        log.info("[notif] markAllSeen query result -> rowsAffected={}, unreadAfter={}", updated, unreadAfter);

        return updated;
    }

    @Transactional
    public void notifyOperationChange(String entityType, Long entityId, String action, String actorUsername) {
        if (!"UPDATE".equalsIgnoreCase(action)) {
            return; // ONLY for update operations
        }

        String normalizedType = entityType == null ? "OPERATION" : entityType.toUpperCase(Locale.ROOT);
        String targetPath = mapEntityTypeToPath(normalizedType);
        if (targetPath == null || entityId == null) {
            return;
        }

        String actor = (actorUsername == null || actorUsername.isBlank()) ? "système" : actorUsername;
        
        String tableLabel = switch (normalizedType) {
            case "PF_PRODUCTION" -> "productions fini";
            case "PSF_PRODUCTION" -> "productions semi fini";
            case "INCOMING" -> "entrees";
            case "OUTGOING" -> "sortis";
            default -> "données";
        };

        String message = "Une modification a été effectuée dans le ligne " + entityId + " du tableau des " + tableLabel;

    repository.save(Objects.requireNonNull(AdminNotification.builder()
                .type("ROW_MODIFIED")
                .message(message)
                .targetPath(targetPath)
                .targetEntityType(normalizedType)
                .targetEntityId(entityId)
                .actor(actor)
                .seen(false)
        .build()));
    }

    private String mapEntityTypeToPath(String entityType) {
        return switch (entityType) {
            case "INCOMING" -> "/departement-1/incoming";
            case "OUTGOING" -> "/logistic/outgoing";
            case "PSF_PRODUCTION" -> "/departement-1/production";
            case "PF_PRODUCTION" -> "/departement-2";
            default -> null;
        };
    }

    private AdminNotificationResponse toResponse(AdminNotification entity) {
        return AdminNotificationResponse.builder()
                .id(entity.getId())
                .type(entity.getType())
                .message(entity.getMessage())
                .seen(entity.isSeen())
                .createdAt(entity.getCreatedAt())
                .targetPath(entity.getTargetPath())
                .targetEntityType(entity.getTargetEntityType())
                .targetEntityId(entity.getTargetEntityId())
                .targetReference(entity.getTargetReference())
                .actor(entity.getActor())
                .build();
    }
}
