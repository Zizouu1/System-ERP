package com.example.backend.modules.admin.notification.controller;

import com.example.backend.modules.admin.notification.dto.AdminNotificationResponse;
import com.example.backend.modules.admin.notification.service.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @GetMapping
    public ResponseEntity<List<AdminNotificationResponse>> listNotifications() {
        return ResponseEntity.ok(adminNotificationService.listNotifications());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("count", adminNotificationService.unreadCount()));
    }

    @PatchMapping("/{id}/seen")
    public ResponseEntity<AdminNotificationResponse> markSeen(@PathVariable Long id) {
        log.info("[notif-api] PATCH /{}/seen received", id);
        AdminNotificationResponse response = adminNotificationService.markSeen(id);
        log.info("[notif-api] PATCH /{}/seen response -> seen={}, targetPath={}", id, response.getSeen(), response.getTargetPath());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/seen-all")
    public ResponseEntity<Map<String, Long>> markAllSeen() {
        log.info("[notif-api] PATCH /seen-all received");
        long updated = adminNotificationService.markAllSeen();
        log.info("[notif-api] PATCH /seen-all response -> updated={}", updated);
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
