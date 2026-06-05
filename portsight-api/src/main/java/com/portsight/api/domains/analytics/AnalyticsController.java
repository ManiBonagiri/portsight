package com.portsight.api.domains.analytics;

import com.portsight.api.domains.analytics.dto.AnalyticsResponse;
import com.portsight.api.domains.analytics.dto.PerformanceResponse;
import com.portsight.api.domains.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    private UUID getUserId(String principal) {
        return UUID.fromString(principal);
    }

    @GetMapping("/portfolio/{id}")
    public ResponseEntity<Map<String, Object>> getPortfolioAnalytics(
            @AuthenticationPrincipal String principal,
            @PathVariable UUID id) {

        UUID userId = getUserId(principal);
        AnalyticsResponse response = analyticsService.getPortfolioAnalytics(userId, id);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @GetMapping("/performance/{id}")
    public ResponseEntity<Map<String, Object>> getPortfolioPerformance(
            @AuthenticationPrincipal String principal,
            @PathVariable UUID id) {

        UUID userId = getUserId(principal);
        PerformanceResponse response = analyticsService.getPortfolioPerformance(userId, id);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }
}
