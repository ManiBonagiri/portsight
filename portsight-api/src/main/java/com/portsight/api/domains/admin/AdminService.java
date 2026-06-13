package com.portsight.api.domains.admin;

import com.portsight.api.domains.audit.AuditLog;
import com.portsight.api.domains.audit.AuditLogRepository;
import com.portsight.api.domains.auth.User;
import com.portsight.api.domains.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    // ── Users ────────────────────────────────────────────────────────────────

    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserSummary)
                .toList();
    }

    private Map<String, Object> toUserSummary(User u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("email", u.getEmail());
        map.put("firstName", u.getFirstName());
        map.put("lastName", u.getLastName());
        map.put("role", u.getRole());
        map.put("status", u.getStatus());
        map.put("createdAt", u.getCreatedAt());
        return map;
    }

    // ── Audit Logs ───────────────────────────────────────────────────────────

    public Page<AuditLog> getAuditLogs(int page, int size, String search) {
        PageRequest pageable = PageRequest.of(page, size);
        if (search != null && !search.isBlank()) {
            return auditLogRepository
                    .findByUserEmailContainingIgnoreCaseOrderByCreatedAtDesc(search, pageable);
        }
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    // ── System Health ────────────────────────────────────────────────────────

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("totalUsers", userRepository.count());
        health.put("totalAuditLogs", auditLogRepository.count());
        health.put("status", "UP");
        return health;
    }
}