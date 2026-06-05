package com.portsight.api.domains.portfolio.dto;

import com.portsight.api.domains.portfolio.enums.RiskProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {
    private UUID id;
    private UUID userId;
    private String portfolioName;
    private RiskProfile riskProfile;
    private String benchmark;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
