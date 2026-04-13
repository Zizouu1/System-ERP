package com.example.backend.modules.admin.notification;

import com.example.backend.modules.admin.notification.entity.AdminNotification;
import com.example.backend.modules.admin.notification.repository.AdminNotificationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@WithMockUser(username = "admin", roles = {"ADMIN"})
class AdminNotificationPersistenceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminNotificationRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void resetData() {
        repository.deleteAll();
    }

    @Test
    void markSeen_shouldPersistInDb_andRemainSeenAfterRefreshApiRead() throws Exception {
        AdminNotification saved = repository.saveAndFlush(AdminNotification.builder()
                .type("TEST")
                .message("mark one")
                .seen(false)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(patch("/api/admin/notifications/{id}/seen", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        AdminNotification fromDb = repository.findById(saved.getId()).orElseThrow();
        assertThat(fromDb.isSeen()).isTrue();

        String listResponse = mockMvc.perform(get("/api/admin/notifications"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> rows = objectMapper.readValue(listResponse, new TypeReference<>() {});
        Map<String, Object> row = rows.stream()
                .filter(r -> Long.valueOf(String.valueOf(r.get("id"))).equals(saved.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(Boolean.parseBoolean(String.valueOf(row.get("seen")))).isTrue();
    }

    @Test
    void markAllSeen_shouldPersistAllUnread_andApiShouldReturnSeenTrueForAll() throws Exception {
        repository.saveAllAndFlush(List.of(
                AdminNotification.builder().type("TEST").message("n1").seen(false).createdAt(LocalDateTime.now()).build(),
                AdminNotification.builder().type("TEST").message("n2").seen(false).createdAt(LocalDateTime.now()).build(),
                AdminNotification.builder().type("TEST").message("n3").seen(true).createdAt(LocalDateTime.now()).build()
        ));

        mockMvc.perform(patch("/api/admin/notifications/seen-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        List<AdminNotification> all = repository.findAll();
        assertThat(all).isNotEmpty();
        assertThat(all).allMatch(AdminNotification::isSeen);

        String listResponse = mockMvc.perform(get("/api/admin/notifications"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> rows = objectMapper.readValue(listResponse, new TypeReference<>() {});
        assertThat(rows).isNotEmpty();
        assertThat(rows).allMatch(r -> Boolean.parseBoolean(String.valueOf(r.get("seen"))));
    }

    @Test
    void sqlNormalization_shouldSetSeenFalseWhereNull() {
        repository.saveAndFlush(AdminNotification.builder()
                .type("TEST")
                .message("normalization")
                .createdAt(LocalDateTime.now())
                .build());

        int normalized = entityManager.createNativeQuery("UPDATE admin_notification SET seen = false WHERE seen IS NULL")
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        long nullCount = ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM admin_notification WHERE seen IS NULL")
                .getSingleResult()).longValue();

        assertThat(normalized).isGreaterThanOrEqualTo(0);
        assertThat(nullCount).isZero();
    }
}
