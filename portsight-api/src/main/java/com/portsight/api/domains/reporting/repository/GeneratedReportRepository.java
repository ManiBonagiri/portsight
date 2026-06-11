package com.portsight.api.domains.reporting.repository;

import com.portsight.api.domains.reporting.entity.GeneratedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, UUID> {

    List<GeneratedReport> findByPortfolioIdOrderByCreatedAtDesc(UUID portfolioId);
}