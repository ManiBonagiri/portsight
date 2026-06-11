package com.portsight.api.domains.reporting.dto;

import java.time.Instant;
import java.util.UUID;

public class ReportResponse {

    private UUID id;
    private UUID portfolioId;
    private String reportType;
    private String status;
    private Instant generatedAt;
    private Instant createdAt;

    public ReportResponse() {
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
        private final ReportResponse r = new ReportResponse();

        public Builder id(UUID v) {
            r.id = v;
            return this;
        }

        public Builder portfolioId(UUID v) {
            r.portfolioId = v;
            return this;
        }

        public Builder reportType(String v) {
            r.reportType = v;
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

        public Builder createdAt(Instant v) {
            r.createdAt = v;
            return this;
        }

        public ReportResponse build() {
            return r;
        }
    }
}