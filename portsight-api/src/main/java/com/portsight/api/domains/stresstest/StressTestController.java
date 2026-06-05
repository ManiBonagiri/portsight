package com.portsight.api.domains.stresstest;

import com.portsight.api.domains.stresstest.dto.StressTestRequest;
import com.portsight.api.domains.stresstest.dto.StressTestResponse;
import com.portsight.api.domains.stresstest.service.StressTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stress-test")
@RequiredArgsConstructor
public class StressTestController {

    private final StressTestService stressTestService;

    private UUID getUserId(String principal) {
        return UUID.fromString(principal);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> runStressTest(
            @AuthenticationPrincipal String principal,
            @Valid @RequestBody StressTestRequest request) {
        
        UUID userId = getUserId(principal);
        StressTestResponse response = stressTestService.runStressTest(userId, request);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }
}
