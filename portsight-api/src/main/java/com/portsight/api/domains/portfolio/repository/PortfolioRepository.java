package com.portsight.api.domains.portfolio.repository;

import com.portsight.api.domains.portfolio.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {
    List<Portfolio> findByUserIdAndStatus(UUID userId, String status);
    List<Portfolio> findByUserId(UUID userId);
}
