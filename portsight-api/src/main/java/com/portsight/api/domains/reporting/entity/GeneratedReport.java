package com.portsight.api.domains.reporting.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "generated_reports")
public class GeneratedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "report_type", nullable = false)
    private String reportType;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "status", nullable = false)
    private String status; // GENERATING, COMPLETED, FAILED

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public GeneratedReport() {
    }

    // ---- Getters ----
    public UUID getId() {
        return id;
    }

    public UUID getPortfolioId() {
        return portfolioId;
    }

    public String getReportType() {
        return reportType;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getStatus() {
        return status;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // ---- Setters ----
    public void setId(UUID id) {
        this.id = id;
    }

    public void setPortfolioId(UUID portfolioId) {
        this.portfolioId = portfolioId;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // ---- Builder ----
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final GeneratedReport r = new GeneratedReport();

        public Builder portfolioId(UUID v) {
            r.portfolioId = v;
            return this;
        }

        public Builder reportType(String v) {
            r.reportType = v;
            return this;
        }

        public Builder storagePath(String v) {
            r.storagePath = v;
            return this;
        }

        public Builder status(String v) {
            r.status = v;
            return this;
        }

        public Builder generatedAt(Instant v) {
            r.generatedAt = v;
            return this;
        }

        public GeneratedReport build() {
            return r;
        }
    }
}