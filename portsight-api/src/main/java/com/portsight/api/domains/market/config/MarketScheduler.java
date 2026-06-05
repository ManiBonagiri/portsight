package com.portsight.api.domains.market.config;

import com.portsight.api.domains.analytics.repository.PortfolioSnapshotRepository;
import com.portsight.api.domains.analytics.service.AnalyticsService;
import com.portsight.api.domains.portfolio.entity.Portfolio;
import com.portsight.api.domains.portfolio.repository.PortfolioRepository;
import com.portsight.api.domains.risk.repository.RiskMetricsRepository;
import com.portsight.api.domains.risk.service.RiskEngineService;
import com.portsight.api.domains.market.service.MarketSimulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.List;

@Configuration
@EnableScheduling
public class MarketScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarketScheduler.class);

    private final MarketSimulationService marketSimulationService;
    private final RiskEngineService riskEngineService;
    private final RiskMetricsRepository riskMetricsRepository;
    private final PortfolioRepository portfolioRepository;
    private final AnalyticsService analyticsService;
    private final PortfolioSnapshotRepository snapshotRepository;

    public MarketScheduler(MarketSimulationService marketSimulationService,
            RiskEngineService riskEngineService,
            RiskMetricsRepository riskMetricsRepository,
            PortfolioRepository portfolioRepository,
            AnalyticsService analyticsService,
            PortfolioSnapshotRepository snapshotRepository) {
        this.marketSimulationService = marketSimulationService;
        this.riskEngineService = riskEngineService;
        this.riskMetricsRepository = riskMetricsRepository;
        this.portfolioRepository = portfolioRepository;
        this.analyticsService = analyticsService;
        this.snapshotRepository = snapshotRepository;
    }

    // ── Startup: generate today's snapshots + calculate risk if empty ─────────
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        generateSnapshotsOnStartup();
        calculateRiskOnStartup();
    }

    private void generateSnapshotsOnStartup() {
        long existing = snapshotRepository.count();
        if (existing > 0) {
            log.info("Portfolio snapshots already exist ({} records) — skipping startup generation", existing);
            return;
        }

        log.info("portfolio_snapshots table is empty — generating today's snapshots on startup...");
        List<Portfolio> portfolios = portfolioRepository.findAll();
        LocalDate today = LocalDate.now();

        int success = 0;
        for (Portfolio portfolio : portfolios) {
            try {
                analyticsService.generateDailySnapshot(portfolio.getId(), today);
                success++;
            } catch (Exception e) {
                log.warn("Snapshot generation failed for portfolio: {} — {}", portfolio.getId(), e.getMessage());
            }
        }
        log.info("Startup snapshot generation complete — {} snapshots created", success);
    }

    private void calculateRiskOnStartup() {
        long existingCount = riskMetricsRepository.count();
        if (existingCount > 0) {
            log.info("Risk metrics already exist ({} records) — skipping startup calculation", existingCount);
            return;
        }

        log.info("risk_metrics table is empty — calculating risk for all portfolios on startup...");
        List<Portfolio> portfolios = portfolioRepository.findAll();

        int success = 0;
        int failed = 0;
        for (Portfolio portfolio : portfolios) {
            try {
                riskEngineService.calculatePortfolioRisk(portfolio.getId());
                success++;
            } catch (Exception e) {
                log.warn("Risk calculation failed for portfolio: {} — {}", portfolio.getId(), e.getMessage());
                failed++;
            }
        }
        log.info("Startup risk calculation complete — success: {}, failed: {}", success, failed);
    }

    // ── Nightly 01:00 — simulate one market day ───────────────────────────────
    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduleDailyMarketSimulation() {
        log.info("Triggering scheduled daily market simulation...");
        marketSimulationService.simulateMarketDay(LocalDate.now());
    }

    // ── Nightly 02:00 — generate portfolio snapshots ──────────────────────────
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduleNightlySnapshots() {
        log.info("Triggering nightly portfolio snapshot generation...");
        List<Portfolio> portfolios = portfolioRepository.findAll();
        LocalDate today = LocalDate.now();
        for (Portfolio portfolio : portfolios) {
            try {
                analyticsService.generateDailySnapshot(portfolio.getId(), today);
            } catch (Exception e) {
                log.warn("Snapshot failed for portfolio: {} — {}", portfolio.getId(), e.getMessage());
            }
        }
        log.info("Nightly snapshot generation complete for {} portfolios", portfolios.size());
    }

    // ── Nightly 03:00 — recalculate risk ─────────────────────────────────────
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduleNightlyRiskCalculation() {
        log.info("Triggering nightly risk recalculation...");
        List<Portfolio> portfolios = portfolioRepository.findAll();
        for (Portfolio portfolio : portfolios) {
            try {
                riskEngineService.calculatePortfolioRisk(portfolio.getId());
            } catch (Exception e) {
                log.warn("Nightly risk calc failed for portfolio: {} — {}", portfolio.getId(), e.getMessage());
            }
        }
        log.info("Nightly risk recalculation complete for {} portfolios", portfolios.size());
    }
}