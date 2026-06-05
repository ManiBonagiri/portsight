package com.portsight.api.domains.risk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "risk_metrics", uniqueConstraints = {@UniqueConstraint(columnNames = {"portfolio_id"})})
public class RiskMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(precision = 19, scale = 4)
    private BigDecimal volatility;

    @Column(precision = 19, scale = 4)
    private BigDecimal beta;

    @Column(name = "sharpe_ratio", precision = 19, scale = 4)
    private BigDecimal sharpeRatio;

    @Column(name = "var_95", precision = 19, scale = 4)
    private BigDecimal var95;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
