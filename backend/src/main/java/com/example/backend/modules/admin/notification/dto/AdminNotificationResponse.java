package com.example.backend.modules.admin.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotificationResponse {

    private Long id;
    private String type;
    private String message;
    private Boolean seen;
    private LocalDateTime createdAt;
    private String targetPath;
    private String targetEntityType;
    private Long targetEntityId;
    private String targetReference;
    private String actor;
}
