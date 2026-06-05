package com.portsight.api.domains.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "portfolio_snapshots", uniqueConstraints = {@UniqueConstraint(columnNames = {"portfolio_id", "snapshot_date"})})
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_value", precision = 19, scale = 4)
    private BigDecimal totalValue;

    @Column(name = "invested_amount", precision = 19, scale = 4)
    private BigDecimal investedAmount;

    @Column(name = "realized_gain", precision = 19, scale = 4)
    private BigDecimal realizedGain;

    @Column(name = "unrealized_gain", precision = 19, scale = 4)
    private BigDecimal unrealizedGain;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
