package com.portsight.api.domains.portfolio;

import com.portsight.api.domains.portfolio.dto.CreatePortfolioRequest;
import com.portsight.api.domains.portfolio.dto.PortfolioResponse;
import com.portsight.api.domains.portfolio.dto.UpdatePortfolioRequest;
import com.portsight.api.domains.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    // TODO: Extract userId from AuthenticationPrincipal (JWT Subject) once SecurityContext is fully integrated.
    // For now, assuming we can parse it from a String principal or a custom UserDetails object.
    // Assuming principal is the subject string (UUID)
    private UUID getUserId(String principal) {
        return UUID.fromString(principal);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPortfolio(
            @AuthenticationPrincipal String principal,
            @Valid @RequestBody CreatePortfolioRequest request) {
        
        UUID userId = getUserId(principal);
        PortfolioResponse response = portfolioService.createPortfolio(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", response));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserPortfolios(@AuthenticationPrincipal String principal) {
        UUID userId = getUserId(principal);
        List<PortfolioResponse> response = portfolioService.getUserPortfolios(userId);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPortfolio(
            @AuthenticationPrincipal String principal,
            @PathVariable UUID id) {
        
        UUID userId = getUserId(principal);
        PortfolioResponse response = portfolioService.getPortfolio(userId, id);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePortfolio(
            @AuthenticationPrincipal String principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePortfolioRequest request) {
        
        UUID userId = getUserId(principal);
        PortfolioResponse response = portfolioService.updatePortfolio(userId, id, request);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> archivePortfolio(
            @AuthenticationPrincipal String principal,
            @PathVariable UUID id) {
        
        UUID userId = getUserId(principal);
        portfolioService.archivePortfolio(userId, id);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("message", "Portfolio archived successfully")));
    }
}
