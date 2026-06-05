package com.portsight.api.domains.portfolio.dto;

import com.portsight.api.domains.portfolio.enums.RiskProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePortfolioRequest {
    @NotBlank(message = "Portfolio name is required")
    private String portfolioName;

    @NotNull(message = "Risk profile is required")
    private RiskProfile riskProfile;

    @NotBlank(message = "Benchmark is required")
    private String benchmark;
}
