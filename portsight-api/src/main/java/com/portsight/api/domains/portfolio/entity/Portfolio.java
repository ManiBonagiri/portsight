package com.portsight.api.domains.portfolio.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.portsight.api.domains.portfolio.enums.RiskProfile;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "portfolios")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "portfolio_name", nullable = false, length = 100)
    private String portfolioName;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_profile", nullable = false, length = 50)
    private RiskProfile riskProfile;

    @Column(name = "benchmark", nullable = false, length = 50)
    private String benchmark;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ---- Constructors ----
    public Portfolio() {}

    public Portfolio(UUID id, UUID userId, String portfolioName, RiskProfile riskProfile,
                     String benchmark, String status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.portfolioName = portfolioName;
        this.riskProfile = riskProfile;
        this.benchmark = benchmark;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ---- Getters ----
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getPortfolioName() { return portfolioName; }
    public RiskProfile getRiskProfile() { return riskProfile; }
    public String getBenchmark() { return benchmark; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // ---- Setters ----
    public void setId(UUID id) { this.id = id; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setPortfolioName(String portfolioName) { this.portfolioName = portfolioName; }
    public void setRiskProfile(RiskProfile riskProfile) { this.riskProfile = riskProfile; }
    public void setBenchmark(String benchmark) { this.benchmark = benchmark; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // ---- Builder ----
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private String portfolioName;
        private RiskProfile riskProfile;
        private String benchmark;
        private String status = "ACTIVE";

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder portfolioName(String portfolioName) { this.portfolioName = portfolioName; return this; }
        public Builder riskProfile(RiskProfile riskProfile) { this.riskProfile = riskProfile; return this; }
        public Builder benchmark(String benchmark) { this.benchmark = benchmark; return this; }
        public Builder status(String status) { this.status = status; return this; }

        public Portfolio build() {
            Portfolio p = new Portfolio();
            p.id = this.id;
            p.userId = this.userId;
            p.portfolioName = this.portfolioName;
            p.riskProfile = this.riskProfile;
            p.benchmark = this.benchmark;
            p.status = this.status;
            return p;
        }
    }
}
