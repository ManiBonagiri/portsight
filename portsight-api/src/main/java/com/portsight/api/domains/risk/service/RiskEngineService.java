package com.portsight.api.domains.risk.service;

import com.portsight.api.domains.asset.entity.Asset;
import com.portsight.api.domains.asset.repository.AssetRepository;
import com.portsight.api.domains.holding.entity.Holding;
import com.portsight.api.domains.holding.repository.HoldingRepository;
import com.portsight.api.domains.market.entity.AssetPriceHistory;
import com.portsight.api.domains.market.repository.AssetPriceHistoryRepository;
import com.portsight.api.domains.portfolio.entity.Portfolio;
import com.portsight.api.domains.portfolio.repository.PortfolioRepository;
import com.portsight.api.domains.risk.dto.RiskResponse;
import com.portsight.api.domains.risk.entity.RiskMetrics;
import com.portsight.api.domains.risk.repository.RiskMetricsRepository;
import com.portsight.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEngineService {

    private static final String BENCHMARK_TICKER = "SIM_NIFTY";
    private static final double RISK_FREE_RATE = 0.065; // 6.5% annual
    private static final int TRADING_DAYS = 252;
    private static final int LOOKBACK_DAYS = 365;

    private final RiskMetricsRepository riskMetricsRepository;
    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final AssetRepository assetRepository;
    private final AssetPriceHistoryRepository priceHistoryRepository;

    // ── Public: calculate and persist risk metrics ────────────────────────────
    @Transactional
    public void calculatePortfolioRisk(UUID portfolioId) {
        log.info("Calculating risk metrics for portfolio: {}", portfolioId);

        portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));

        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        if (holdings.isEmpty()) {
            log.warn("No holdings found for portfolio: {} — skipping risk calculation", portfolioId);
            return;
        }

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(LOOKBACK_DAYS);

        // Build daily portfolio values
        List<Double> portfolioValues = buildDailyPortfolioValues(holdings, from, to);

        if (portfolioValues.size() < 10) {
            log.warn("Insufficient price history for portfolio: {} — using profile-based fallback", portfolioId);
            persistFallback(portfolioId);
            return;
        }

        // Daily returns
        List<Double> portfolioReturns = dailyReturns(portfolioValues);

        // Benchmark returns
        List<Double> benchmarkReturns = buildBenchmarkReturns(from, to);

        // Calculations
        double volatility = annualizedVolatility(portfolioReturns);
        double beta = computeBeta(portfolioReturns, benchmarkReturns);
        double annualReturn = annualizedReturn(portfolioValues);
        double sharpe = computeSharpe(annualReturn, volatility);
        double var95 = computeVaR95(portfolioReturns);

        RiskMetrics metrics = riskMetricsRepository.findByPortfolioId(portfolioId)
                .orElse(RiskMetrics.builder().portfolioId(portfolioId).build());

        metrics.setVolatility(bd(volatility));
        metrics.setBeta(bd(beta));
        metrics.setSharpeRatio(bd(sharpe));
        metrics.setVar95(bd(var95));

        riskMetricsRepository.save(metrics);
        log.info("Risk metrics saved for portfolio: {} | vol={} beta={} sharpe={} var95={}",
                portfolioId, volatility, beta, sharpe, var95);
    }

    // ── Public: fetch persisted metrics ──────────────────────────────────────
    @Transactional(readOnly = true)
    public RiskResponse getRiskMetrics(UUID userId, UUID portfolioId) {
        verifyPortfolioOwnership(userId, portfolioId);

        RiskMetrics metrics = riskMetricsRepository.findByPortfolioId(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Risk metrics not calculated for portfolio: " + portfolioId));

        return RiskResponse.builder()
                .volatility(metrics.getVolatility())
                .beta(metrics.getBeta())
                .sharpeRatio(metrics.getSharpeRatio())
                .var95(metrics.getVar95())
                .build();
    }

    // ── Build daily portfolio value series ────────────────────────────────────
    private List<Double> buildDailyPortfolioValues(List<Holding> holdings,
            LocalDate from, LocalDate to) {
        // Collect price histories per asset
        Map<UUID, List<AssetPriceHistory>> priceMap = new HashMap<>();
        for (Holding h : holdings) {
            List<AssetPriceHistory> history = priceHistoryRepository
                    .findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
                            h.getAssetId(), from, to);
            if (!history.isEmpty()) {
                priceMap.put(h.getAssetId(), history);
            }
        }

        if (priceMap.isEmpty())
            return Collections.emptyList();

        // Find common date range across all assets
        Set<LocalDate> allDates = new TreeSet<>();
        for (List<AssetPriceHistory> history : priceMap.values()) {
            for (AssetPriceHistory aph : history) {
                allDates.add(aph.getRecordDate());
            }
        }

        // Build index maps for quick lookup
        Map<UUID, Map<LocalDate, Double>> priceByAssetDate = new HashMap<>();
        for (Map.Entry<UUID, List<AssetPriceHistory>> e : priceMap.entrySet()) {
            Map<LocalDate, Double> datePrice = new HashMap<>();
            for (AssetPriceHistory aph : e.getValue()) {
                datePrice.put(aph.getRecordDate(), aph.getClosingPrice().doubleValue());
            }
            priceByAssetDate.put(e.getKey(), datePrice);
        }

        // For each date, sum quantity × price across all holdings
        List<Double> values = new ArrayList<>();
        for (LocalDate date : allDates) {
            double dayValue = 0.0;
            boolean hasAllPrices = true;
            for (Holding h : holdings) {
                Map<LocalDate, Double> datePrice = priceByAssetDate.get(h.getAssetId());
                if (datePrice == null || !datePrice.containsKey(date)) {
                    hasAllPrices = false;
                    break;
                }
                dayValue += h.getQuantity().doubleValue() * datePrice.get(date);
            }
            if (hasAllPrices)
                values.add(dayValue);
        }

        return values;
    }

    // ── Build benchmark daily returns ─────────────────────────────────────────
    private List<Double> buildBenchmarkReturns(LocalDate from, LocalDate to) {
        Optional<Asset> benchmarkAsset = assetRepository.findByTicker(BENCHMARK_TICKER);
        if (benchmarkAsset.isEmpty()) {
            log.warn("Benchmark {} not found — beta will default to 1.0", BENCHMARK_TICKER);
            return Collections.emptyList();
        }

        List<AssetPriceHistory> history = priceHistoryRepository.findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
                benchmarkAsset.get().getId(), from, to);

        List<Double> prices = new ArrayList<>();
        for (AssetPriceHistory aph : history) {
            prices.add(aph.getClosingPrice().doubleValue());
        }

        return dailyReturns(prices);
    }

    // ── Math helpers ──────────────────────────────────────────────────────────

    // daily return[i] = (price[i+1] - price[i]) / price[i]
    private List<Double> dailyReturns(List<Double> values) {
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < values.size(); i++) {
            double prev = values.get(i - 1);
            if (prev != 0.0) {
                returns.add((values.get(i) - prev) / prev);
            }
        }
        return returns;
    }

    // Annualized volatility = std dev of daily returns × √252
    private double annualizedVolatility(List<Double> returns) {
        if (returns.size() < 2)
            return 0.0;
        double mean = mean(returns);
        double variance = 0.0;
        for (double r : returns) {
            variance += Math.pow(r - mean, 2);
        }
        variance /= (returns.size() - 1);
        return Math.sqrt(variance) * Math.sqrt(TRADING_DAYS) * 100; // as percentage
    }

    // Beta = Cov(portfolio, benchmark) / Var(benchmark)
    private double computeBeta(List<Double> portfolioReturns, List<Double> benchmarkReturns) {
        if (benchmarkReturns.isEmpty())
            return 1.0;

        int n = Math.min(portfolioReturns.size(), benchmarkReturns.size());
        if (n < 2)
            return 1.0;

        List<Double> p = portfolioReturns.subList(0, n);
        List<Double> b = benchmarkReturns.subList(0, n);

        double meanP = mean(p);
        double meanB = mean(b);

        double cov = 0.0;
        double varB = 0.0;
        for (int i = 0; i < n; i++) {
            cov += (p.get(i) - meanP) * (b.get(i) - meanB);
            varB += Math.pow(b.get(i) - meanB, 2);
        }

        if (varB == 0.0)
            return 1.0;
        return cov / varB;
    }

    // Annualized return = (endValue / startValue)^(252/n) - 1
    private double annualizedReturn(List<Double> values) {
        if (values.size() < 2)
            return 0.0;
        double start = values.get(0);
        double end = values.get(values.size() - 1);
        if (start <= 0.0)
            return 0.0;
        int n = values.size();
        return (Math.pow(end / start, (double) TRADING_DAYS / n) - 1.0);
    }

    // Sharpe = (annualReturn - riskFreeRate) / volatility
    private double computeSharpe(double annualReturn, double volatility) {
        if (volatility == 0.0)
            return 0.0;
        double volDecimal = volatility / 100.0; // convert % back to decimal
        return (annualReturn - RISK_FREE_RATE) / volDecimal;
    }

    // VaR 95% = 5th percentile of daily returns (as %)
    private double computeVaR95(List<Double> returns) {
        if (returns.isEmpty())
            return 0.0;
        List<Double> sorted = new ArrayList<>(returns);
        Collections.sort(sorted);
        int index = (int) Math.floor(0.05 * sorted.size());
        return sorted.get(Math.max(index, 0)) * 100; // as percentage
    }

    private double mean(List<Double> values) {
        double sum = 0.0;
        for (double v : values)
            sum += v;
        return sum / values.size();
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    // Fallback when price history is missing — uses risk profile defaults
    private void persistFallback(UUID portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId).orElseThrow();
        BigDecimal vol, beta, sharpe, var95;

        switch (portfolio.getRiskProfile()) {
            case CONSERVATIVE:
                vol = bd(5.2);
                beta = bd(0.6);
                sharpe = bd(1.1);
                var95 = bd(-3.5);
                break;
            case MODERATE:
                vol = bd(12.5);
                beta = bd(1.0);
                sharpe = bd(1.45);
                var95 = bd(-8.2);
                break;
            case AGGRESSIVE:
            case VERY_AGGRESSIVE:
            default:
                vol = bd(18.4);
                beta = bd(1.5);
                sharpe = bd(1.6);
                var95 = bd(-14.1);
                break;
        }

        RiskMetrics metrics = riskMetricsRepository.findByPortfolioId(portfolioId)
                .orElse(RiskMetrics.builder().portfolioId(portfolioId).build());

        metrics.setVolatility(vol);
        metrics.setBeta(beta);
        metrics.setSharpeRatio(sharpe);
        metrics.setVar95(var95);
        riskMetricsRepository.save(metrics);
    }

    private void verifyPortfolioOwnership(UUID userId, UUID portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));
        if (!portfolio.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Portfolio not found: " + portfolioId);
        }
    }
}