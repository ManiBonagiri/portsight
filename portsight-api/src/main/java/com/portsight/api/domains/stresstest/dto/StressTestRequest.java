package com.portsight.api.domains.stresstest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StressTestRequest {
    @NotNull(message = "Portfolio ID is required")
    private UUID portfolioId;

    @NotBlank(message = "Scenario is required")
    private String scenario; // e.g., MARKET_CRASH_20, TECH_CRASH_15, RATE_HIKE_2
}
