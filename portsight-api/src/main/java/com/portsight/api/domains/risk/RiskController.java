package com.portsight.api.domains.risk;

import com.portsight.api.domains.risk.dto.RiskResponse;
import com.portsight.api.domains.risk.service.RiskEngineService;
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
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskEngineService riskEngineService;

    private UUID getUserId(String principal) {
        return UUID.fromString(principal);
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<Map<String, Object>> getRiskMetrics(
            @AuthenticationPrincipal String principal,
            @PathVariable UUID portfolioId) {
        
        UUID userId = getUserId(principal);
        RiskResponse response = riskEngineService.getRiskMetrics(userId, portfolioId);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }
}
