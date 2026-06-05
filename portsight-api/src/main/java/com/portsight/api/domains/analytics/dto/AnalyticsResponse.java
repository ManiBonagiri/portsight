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
public class AnalyticsResponse {
    private BigDecimal portfolioValue;
    private BigDecimal investedAmount;
    private BigDecimal gainLoss;
    private BigDecimal returnPercentage;
}
