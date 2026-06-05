package com.portsight.api.domains.risk.repository;

import com.portsight.api.domains.risk.entity.RiskMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskMetricsRepository extends JpaRepository<RiskMetrics, UUID> {
    Optional<RiskMetrics> findByPortfolioId(UUID portfolioId);
}
