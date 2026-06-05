package com.portsight.api.domains.stresstest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StressTestResponse {
    private String scenario;
    private BigDecimal preStressValue;
    private BigDecimal postStressValue;
    private BigDecimal impactAmount;
    private BigDecimal impactPercent;
}
