package com.portsight.api.domains.analytics.repository;

import com.portsight.api.domains.analytics.entity.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, UUID> {
    List<PortfolioSnapshot> findByPortfolioIdOrderBySnapshotDateAsc(UUID portfolioId);

    Optional<PortfolioSnapshot> findByPortfolioIdAndSnapshotDate(UUID portfolioId, LocalDate snapshotDate);

    List<PortfolioSnapshot> findByPortfolioIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            UUID portfolioId, LocalDate from, LocalDate to);

    Optional<PortfolioSnapshot> findTopByPortfolioIdOrderBySnapshotDateDesc(UUID portfolioId);
}