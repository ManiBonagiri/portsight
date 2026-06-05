package com.portsight.api.domains.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceResponse {
    private BigDecimal dailyReturn;
    private BigDecimal monthlyReturn;
    private BigDecimal annualReturn;
    private BigDecimal cagr;
}
