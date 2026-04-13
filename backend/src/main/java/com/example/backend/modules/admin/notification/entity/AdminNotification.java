package com.example.backend.modules.admin.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, length = 1200)
    private String message;

    @Column(nullable = false)
    @Builder.Default
    private boolean seen = false;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private String targetPath;

    private String targetEntityType;

    private Long targetEntityId;

    private String targetReference;

    @Column(length = 300)
    private String eventKey;

    @Column(length = 100)
    private String actor;
}
