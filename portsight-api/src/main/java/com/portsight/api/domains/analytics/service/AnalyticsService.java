package com.portsight.api.domains.analytics.service;

import com.portsight.api.domains.analytics.dto.AnalyticsResponse;
import com.portsight.api.domains.analytics.dto.PerformanceResponse;
import com.portsight.api.domains.analytics.entity.PortfolioSnapshot;
import com.portsight.api.domains.analytics.repository.PortfolioSnapshotRepository;
import com.portsight.api.domains.asset.entity.Asset;
import com.portsight.api.domains.asset.repository.AssetRepository;
import com.portsight.api.domains.holding.entity.Holding;
import com.portsight.api.domains.holding.repository.HoldingRepository;
import com.portsight.api.domains.market.entity.AssetPriceHistory;
import com.portsight.api.domains.market.repository.AssetPriceHistoryRepository;
import com.portsight.api.domains.portfolio.entity.Portfolio;
import com.portsight.api.domains.portfolio.repository.PortfolioRepository;
import com.portsight.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final AssetRepository assetRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final AssetPriceHistoryRepository priceHistoryRepository;

    // ── Generate daily snapshot for one portfolio ─────────────────────────────
    @Transactional
    public void generateDailySnapshot(UUID portfolioId, LocalDate date) {
        log.info("Generating daily snapshot for portfolio: {} on date: {}", portfolioId, date);

        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        if (holdings.isEmpty())
            return;

        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal investedAmount = BigDecimal.ZERO;

        for (Holding holding : holdings) {
            Asset asset = assetRepository.findById(holding.getAssetId()).orElse(null);
            BigDecimal price = (asset != null && asset.getCurrentPrice() != null)
                    ? asset.getCurrentPrice()
                    : holding.getAveragePrice();
            totalValue = totalValue.add(holding.getQuantity().multiply(price));
            investedAmount = investedAmount.add(holding.getQuantity().multiply(holding.getAveragePrice()));
        }

        BigDecimal unrealizedGain = totalValue.subtract(investedAmount);

        PortfolioSnapshot snapshot = snapshotRepository
                .findByPortfolioIdAndSnapshotDate(portfolioId, date)
                .orElse(PortfolioSnapshot.builder()
                        .portfolioId(portfolioId)
                        .snapshotDate(date)
                        .build());

        snapshot.setTotalValue(totalValue);
        snapshot.setInvestedAmount(investedAmount);
        snapshot.setUnrealizedGain(unrealizedGain);
        snapshot.setRealizedGain(BigDecimal.ZERO);

        snapshotRepository.save(snapshot);
    }

    // ── Get portfolio analytics (real-time) ───────────────────────────────────
    @Transactional(readOnly = true)
    public AnalyticsResponse getPortfolioAnalytics(UUID userId, UUID portfolioId) {
        verifyPortfolioOwnership(userId, portfolioId);

        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);

        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal investedAmount = BigDecimal.ZERO;

        for (Holding holding : holdings) {
            Asset asset = assetRepository.findById(holding.getAssetId()).orElse(null);
            BigDecimal price = (asset != null && asset.getCurrentPrice() != null)
                    ? asset.getCurrentPrice()
                    : holding.getAveragePrice();
            totalValue = totalValue.add(holding.getQuantity().multiply(price));
            investedAmount = investedAmount.add(holding.getQuantity().multiply(holding.getAveragePrice()));
        }

        BigDecimal gainLoss = totalValue.subtract(investedAmount);
        BigDecimal returnPercentage = BigDecimal.ZERO;
        if (investedAmount.compareTo(BigDecimal.ZERO) > 0) {
            returnPercentage = gainLoss
                    .divide(investedAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return AnalyticsResponse.builder()
                .portfolioValue(totalValue)
                .investedAmount(investedAmount)
                .gainLoss(gainLoss)
                .returnPercentage(returnPercentage)
                .build();
    }

    // ── Get portfolio performance (real CAGR from price history) ─────────────
    @Transactional(readOnly = true)
    public PerformanceResponse getPortfolioPerformance(UUID userId, UUID portfolioId) {
        verifyPortfolioOwnership(userId, portfolioId);

        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        if (holdings.isEmpty()) {
            return zeroPerfResponse();
        }

        LocalDate today = LocalDate.now();
        LocalDate oneYear = today.minusYears(1);

        // Build daily portfolio values over the past year
        List<Double> portfolioValues = buildDailyPortfolioValues(holdings, oneYear, today);

        if (portfolioValues.size() < 5) {
            log.warn("Insufficient price history for performance calc — portfolio: {}", portfolioId);
            return zeroPerfResponse();
        }

        double startValue = portfolioValues.get(0);
        double endValue = portfolioValues.get(portfolioValues.size() - 1);
        int days = portfolioValues.size();

        // Annual return = (end/start)^(252/days) - 1
        double annualReturn = 0.0;
        if (startValue > 0) {
            annualReturn = (Math.pow(endValue / startValue, 252.0 / days) - 1.0) * 100;
        }

        // CAGR over available history
        double years = days / 252.0;
        double cagr = 0.0;
        if (startValue > 0 && years > 0) {
            cagr = (Math.pow(endValue / startValue, 1.0 / years) - 1.0) * 100;
        }

        // Daily return = last day change
        double dailyReturn = 0.0;
        if (portfolioValues.size() >= 2) {
            double prev = portfolioValues.get(portfolioValues.size() - 2);
            if (prev > 0)
                dailyReturn = ((endValue - prev) / prev) * 100;
        }

        // Monthly return — value 30 days ago
        double monthlyReturn = 0.0;
        int monthIdx = Math.max(0, portfolioValues.size() - 22); // ~22 trading days
        double monthStart = portfolioValues.get(monthIdx);
        if (monthStart > 0) {
            monthlyReturn = ((endValue - monthStart) / monthStart) * 100;
        }

        return PerformanceResponse.builder()
                .dailyReturn(bd(dailyReturn))
                .monthlyReturn(bd(monthlyReturn))
                .annualReturn(bd(annualReturn))
                .cagr(bd(cagr))
                .build();
    }

    // ── Build daily portfolio value series from price history ─────────────────
    private List<Double> buildDailyPortfolioValues(List<Holding> holdings,
            LocalDate from, LocalDate to) {
        Map<UUID, Map<LocalDate, Double>> priceByAssetDate = new HashMap<>();

        for (Holding h : holdings) {
            List<AssetPriceHistory> history = priceHistoryRepository
                    .findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
                            h.getAssetId(), from, to);
            Map<LocalDate, Double> datePrice = new HashMap<>();
            for (AssetPriceHistory aph : history) {
                datePrice.put(aph.getRecordDate(), aph.getClosingPrice().doubleValue());
            }
            if (!datePrice.isEmpty()) {
                priceByAssetDate.put(h.getAssetId(), datePrice);
            }
        }

        if (priceByAssetDate.isEmpty())
            return Collections.emptyList();

        // Collect all dates
        Set<LocalDate> allDates = new TreeSet<>();
        for (Map<LocalDate, Double> m : priceByAssetDate.values()) {
            allDates.addAll(m.keySet());
        }

        List<Double> values = new ArrayList<>();
        for (LocalDate date : allDates) {
            double dayValue = 0.0;
            boolean complete = true;
            for (Holding h : holdings) {
                Map<LocalDate, Double> dp = priceByAssetDate.get(h.getAssetId());
                if (dp == null || !dp.containsKey(date)) {
                    complete = false;
                    break;
                }
                dayValue += h.getQuantity().doubleValue() * dp.get(date);
            }
            if (complete)
                values.add(dayValue);
        }

        return values;
    }

    private PerformanceResponse zeroPerfResponse() {
        return PerformanceResponse.builder()
                .dailyReturn(BigDecimal.ZERO)
                .monthlyReturn(BigDecimal.ZERO)
                .annualReturn(BigDecimal.ZERO)
                .cagr(BigDecimal.ZERO)
                .build();
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private void verifyPortfolioOwnership(UUID userId, UUID portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));
        if (!portfolio.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Portfolio not found: " + portfolioId);
        }
    }
}