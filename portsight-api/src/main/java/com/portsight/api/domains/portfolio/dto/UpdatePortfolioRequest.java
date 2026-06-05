package com.portsight.api.domains.portfolio.dto;

import com.portsight.api.domains.portfolio.enums.RiskProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePortfolioRequest {
    private String portfolioName;
    private RiskProfile riskProfile;
    private String benchmark;
}
