package com.portsight.api.domains.reporting.service;

import com.portsight.api.domains.analytics.entity.PortfolioSnapshot;
import com.portsight.api.domains.analytics.repository.PortfolioSnapshotRepository;
import com.portsight.api.domains.holding.dto.HoldingResponse;
import com.portsight.api.domains.holding.service.HoldingService;
import com.portsight.api.domains.portfolio.entity.Portfolio;
import com.portsight.api.domains.portfolio.repository.PortfolioRepository;
import com.portsight.api.domains.reporting.dto.ReportResponse;
import com.portsight.api.domains.reporting.entity.GeneratedReport;
import com.portsight.api.domains.reporting.repository.GeneratedReportRepository;
import com.portsight.api.domains.risk.entity.RiskMetrics;
import com.portsight.api.domains.risk.repository.RiskMetricsRepository;
import com.portsight.api.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportingService {

    private static final Logger log = LoggerFactory.getLogger(ReportingService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy")
            .withZone(ZoneId.systemDefault());

    private final GeneratedReportRepository reportRepository;
    private final PortfolioRepository portfolioRepository;
    private final RiskMetricsRepository riskMetricsRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final HoldingService holdingService;
    private final PdfReportBuilder pdfBuilder;
    private final MinioService minioService;

    public ReportingService(
            GeneratedReportRepository reportRepository,
            PortfolioRepository portfolioRepository,
            RiskMetricsRepository riskMetricsRepository,
            PortfolioSnapshotRepository snapshotRepository,
            HoldingService holdingService,
            PdfReportBuilder pdfBuilder,
            MinioService minioService) {
        this.reportRepository = reportRepository;
        this.portfolioRepository = portfolioRepository;
        this.riskMetricsRepository = riskMetricsRepository;
        this.snapshotRepository = snapshotRepository;
        this.holdingService = holdingService;
        this.pdfBuilder = pdfBuilder;
        this.minioService = minioService;
    }

    // ── Generate ──────────────────────────────────────────────────────────────

    public ReportResponse generateReport(UUID userId, UUID portfolioId, String reportType) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));

        GeneratedReport report = GeneratedReport.builder()
                .portfolioId(portfolioId)
                .reportType(reportType.toUpperCase())
                .status("GENERATING")
                .build();
        report = reportRepository.save(report);

        try {
            Map<String, String> data = buildReportData(userId, portfolioId, reportType, portfolio);
            byte[] pdf = pdfBuilder.generate(reportType, portfolio.getPortfolioName(), data);

            String objectName = "reports/" + portfolioId + "/" +
                    reportType.toLowerCase() + "_" + report.getId() + ".pdf";
            minioService.uploadReport(objectName, pdf);

            report.setStatus("COMPLETED");
            report.setStoragePath(objectName);
            report.setGeneratedAt(Instant.now());
            report = reportRepository.save(report);

            log.info("Report generated: id={} type={} portfolio={}", report.getId(), reportType, portfolioId);

        } catch (Exception e) {
            log.error("Report generation failed for portfolio={} type={}: {}", portfolioId, reportType, e.getMessage());
            report.setStatus("FAILED");
            report = reportRepository.save(report);
        }

        return toResponse(report);
    }

    // ── List ──────────────────────────────────────────────────────────────────

    public List<ReportResponse> getReports(UUID portfolioId) {
        return reportRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Download ──────────────────────────────────────────────────────────────

    public byte[] downloadReport(UUID reportId) {
        GeneratedReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));

        if (!"COMPLETED".equals(report.getStatus())) {
            throw new IllegalStateException("Report is not ready. Status: " + report.getStatus());
        }

        try {
            return minioService.downloadReport(report.getStoragePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download report: " + e.getMessage(), e);
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void deleteReport(UUID reportId) {
        GeneratedReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));
        reportRepository.delete(report);
    }

    // ── Data builders ─────────────────────────────────────────────────────────

    private Map<String, String> buildReportData(UUID userId, UUID portfolioId,
            String reportType, Portfolio portfolio) {
        Optional<RiskMetrics> riskOpt = riskMetricsRepository.findByPortfolioId(portfolioId);
        Optional<PortfolioSnapshot> snapOpt = snapshotRepository
                .findTopByPortfolioIdOrderBySnapshotDateDesc(portfolioId);

        return switch (reportType.toUpperCase()) {
            case "RISK" -> buildRiskData(portfolio, riskOpt);
            case "PERFORMANCE" -> buildPerformanceData(portfolio, snapOpt);
            case "ALLOCATION" -> buildAllocationData(userId, portfolioId, portfolio);
            default -> buildPortfolioData(portfolio, snapOpt, riskOpt); // PORTFOLIO
        };
    }

    private Map<String, String> buildPortfolioData(Portfolio portfolio,
            Optional<PortfolioSnapshot> snapOpt,
            Optional<RiskMetrics> riskOpt) {

        Map<String, String> data = new LinkedHashMap<>();
        data.put("Portfolio Name", portfolio.getPortfolioName());
        data.put("Risk Profile", portfolio.getRiskProfile() != null ? portfolio.getRiskProfile().name() : "—");
        data.put("Benchmark", nvl(portfolio.getBenchmark()));
        data.put("Report Date", FMT.format(Instant.now()));

        snapOpt.ifPresent(s -> {
            data.put("Portfolio Value", formatINR(s.getTotalValue()));
            data.put("Invested Amount", formatINR(s.getInvestedAmount()));
            data.put("Unrealised Gain/Loss", formatINR(s.getUnrealizedGain()));
            data.put("Snapshot Date", s.getSnapshotDate() != null ? s.getSnapshotDate().toString() : "—");
        });

        riskOpt.ifPresent(r -> {
            data.put("Volatility (Annual)", pct(r.getVolatility()));
            data.put("Beta", plain(r.getBeta()));
            data.put("Sharpe Ratio", plain(r.getSharpeRatio()));
            data.put("VaR 95% (Daily)", pct(r.getVar95()));
        });

        return data;
    }

    private Map<String, String> buildRiskData(Portfolio portfolio,
            Optional<RiskMetrics> riskOpt) {

        Map<String, String> data = new LinkedHashMap<>();
        data.put("Portfolio Name", portfolio.getPortfolioName());
        data.put("Risk Profile", portfolio.getRiskProfile() != null ? portfolio.getRiskProfile().name() : "—");
        data.put("Report Date", FMT.format(Instant.now()));

        if (riskOpt.isPresent()) {
            RiskMetrics r = riskOpt.get();
            data.put("Volatility (Annual)", pct(r.getVolatility()));
            data.put("Beta", plain(r.getBeta()));
            data.put("Sharpe Ratio", plain(r.getSharpeRatio()));
            data.put("VaR 95% (Daily)", pct(r.getVar95()));
        } else {
            data.put("Risk Metrics", "Not yet calculated");
        }

        return data;
    }

    private Map<String, String> buildPerformanceData(Portfolio portfolio,
            Optional<PortfolioSnapshot> snapOpt) {

        Map<String, String> data = new LinkedHashMap<>();
        data.put("Portfolio Name", portfolio.getPortfolioName());
        data.put("Benchmark", nvl(portfolio.getBenchmark()));
        data.put("Report Date", FMT.format(Instant.now()));

        snapOpt.ifPresent(s -> {
            data.put("Portfolio Value", formatINR(s.getTotalValue()));
            data.put("Invested Amount", formatINR(s.getInvestedAmount()));
            data.put("Unrealised Gain/Loss", formatINR(s.getUnrealizedGain()));
            data.put("Realised Gain", formatINR(s.getRealizedGain()));

            if (s.getInvestedAmount() != null &&
                    s.getInvestedAmount().compareTo(BigDecimal.ZERO) > 0 &&
                    s.getUnrealizedGain() != null) {
                BigDecimal returnPct = s.getUnrealizedGain()
                        .divide(s.getInvestedAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
                data.put("Total Return %", returnPct.toPlainString() + "%");
            }

            data.put("Snapshot Date", s.getSnapshotDate() != null ? s.getSnapshotDate().toString() : "—");
        });

        return data;
    }

    private Map<String, String> buildAllocationData(UUID userId, UUID portfolioId,
            Portfolio portfolio) {

        Map<String, String> data = new LinkedHashMap<>();
        data.put("Portfolio Name", portfolio.getPortfolioName());
        data.put("Report Date", FMT.format(Instant.now()));

        try {
            List<HoldingResponse> holdings = holdingService.getPortfolioHoldings(userId, portfolioId);

            BigDecimal totalValue = holdings.stream()
                    .map(h -> h.getCurrentValue() != null ? h.getCurrentValue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Group by sector
            Map<String, BigDecimal> sectorMap = new LinkedHashMap<>();
            for (HoldingResponse h : holdings) {
                String sector = (h.getAsset() != null && h.getAsset().getSector() != null)
                        ? h.getAsset().getSector()
                        : "Other";
                sectorMap.merge(sector,
                        h.getCurrentValue() != null ? h.getCurrentValue() : BigDecimal.ZERO,
                        BigDecimal::add);
            }

            if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
                sectorMap.entrySet().stream()
                        .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                        .forEach(e -> {
                            BigDecimal pct = e.getValue()
                                    .divide(totalValue, 4, RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal("100"))
                                    .setScale(1, RoundingMode.HALF_UP);
                            data.put(e.getKey() + " Allocation", pct.toPlainString() + "%");
                        });
                data.put("Total Portfolio Value", formatINR(totalValue));
                data.put("Total Positions", String.valueOf(holdings.size()));
            }
        } catch (Exception e) {
            log.warn("Could not build allocation data: {}", e.getMessage());
            data.put("Allocation Data", "Unable to calculate");
        }

        return data;
    }

    // ── Formatting helpers ────────────────────────────────────────────────────

    private String formatINR(BigDecimal value) {
        if (value == null)
            return "—";
        return "₹" + String.format("%,.0f", value);
    }

    private String pct(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%" : "—";
    }

    private String plain(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP).toPlainString() : "—";
    }

    private String nvl(String value) {
        return value != null ? value : "—";
    }

    private ReportResponse toResponse(GeneratedReport r) {
        return ReportResponse.builder()
                .id(r.getId())
                .portfolioId(r.getPortfolioId())
                .reportType(r.getReportType())
                .status(r.getStatus())
                .generatedAt(r.getGeneratedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}