package com.portsight.api.domains.holding;

import com.portsight.api.domains.holding.dto.AddHoldingRequest;
import com.portsight.api.domains.holding.dto.HoldingResponse;
import com.portsight.api.domains.holding.service.HoldingService;
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
@RequestMapping("/api/v1/portfolios/{portfolioId}/holdings")
@RequiredArgsConstructor
public class HoldingController {

    private final HoldingService holdingService;

    // TODO: Extract userId from AuthenticationPrincipal (JWT Subject) once SecurityContext is fully integrated.
    private UUID getUserId(String principal) {
        return UUID.fromString(principal);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addHolding(
            @AuthenticationPrincipal String principal,
            @PathVariable UUID portfolioId,
            @Valid @RequestBody AddHoldingRequest request) {
        
        UUID userId = getUserId(principal);
        HoldingResponse response = holdingService.addHolding(userId, portfolioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", response));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPortfolioHoldings(
            @AuthenticationPrincipal String principal,
            @PathVariable UUID portfolioId) {
        
        UUID userId = getUserId(principal);
        List<HoldingResponse> response = holdingService.getPortfolioHoldings(userId, portfolioId);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }
}
