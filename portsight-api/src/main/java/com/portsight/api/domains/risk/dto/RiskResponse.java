package com.portsight.api.domains.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskResponse {
    private BigDecimal volatility;
    private BigDecimal beta;
    private BigDecimal sharpeRatio;
    private BigDecimal var95;
}
