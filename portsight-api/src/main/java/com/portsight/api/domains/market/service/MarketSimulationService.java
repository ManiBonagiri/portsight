package com.portsight.api.domains.market.service;

import com.portsight.api.domains.asset.entity.Asset;
import com.portsight.api.domains.asset.enums.AssetType;
import com.portsight.api.domains.asset.repository.AssetRepository;
import com.portsight.api.domains.market.entity.AssetPriceHistory;
import com.portsight.api.domains.market.repository.AssetPriceHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class MarketSimulationService {

    private static final Logger log = LoggerFactory.getLogger(MarketSimulationService.class);

    private final AssetRepository assetRepository;
    private final AssetPriceHistoryRepository assetPriceHistoryRepository;
    private final Random random = new Random(42); // Fixed seed for reproducibility

    public MarketSimulationService(AssetRepository assetRepository,
            AssetPriceHistoryRepository assetPriceHistoryRepository) {
        this.assetRepository = assetRepository;
        this.assetPriceHistoryRepository = assetPriceHistoryRepository;
    }

    // ─── Advance one day (existing functionality) ─────────────────────────────
    @Transactional
    public void simulateMarketDay(LocalDate simulationDate) {
        log.info("Starting market simulation for date: {}", simulationDate);

        List<Asset> allAssets = assetRepository.findAll();
        double marketFactor = 1.0 + ((random.nextDouble() - 0.5) * 0.04);

        for (Asset asset : allAssets) {
            BigDecimal currentPrice = asset.getCurrentPrice();
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                currentPrice = BigDecimal.valueOf(100.0);
            }

            double trend = 1.0001;
            double volatilityFactor = getVolatilityForAssetType(asset);
            double randomVolatility = 1.0 + ((random.nextDouble() - 0.5) * volatilityFactor);
            double priceMultiplier = trend * randomVolatility * marketFactor;

            BigDecimal newPrice = currentPrice.multiply(BigDecimal.valueOf(priceMultiplier))
                    .setScale(4, RoundingMode.HALF_UP);

            if (newPrice.compareTo(BigDecimal.valueOf(0.01)) < 0) {
                newPrice = BigDecimal.valueOf(0.01);
            }

            asset.setCurrentPrice(newPrice);
            assetRepository.save(asset);

            AssetPriceHistory history = AssetPriceHistory.builder()
                    .assetId(asset.getId())
                    .recordDate(simulationDate)
                    .closingPrice(newPrice)
                    .build();
            assetPriceHistoryRepository.save(history);
        }

        log.info("Completed market simulation for {} assets.", allAssets.size());
    }

    // ─── Generate 5-year historical price data ────────────────────────────────
    @Transactional
    public void generateHistoricalPrices() {
        List<Asset> allAssets = assetRepository.findAll();
        if (allAssets.isEmpty()) {
            log.warn("No assets found — skipping historical price generation.");
            return;
        }

        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusYears(5);

        // Collect all trading days (Mon–Fri only)
        List<LocalDate> tradingDays = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            int dow = cursor.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
            if (dow <= 5)
                tradingDays.add(cursor);
            cursor = cursor.plusDays(1);
        }

        log.info("Generating historical prices for {} assets over {} trading days...",
                allAssets.size(), tradingDays.size());

        // ── Sector volatility multipliers ─────────────────────────────────────
        Map<String, Double> sectorVol = new HashMap<>();
        sectorVol.put("Technology", 0.018);
        sectorVol.put("Banking", 0.015);
        sectorVol.put("Healthcare", 0.013);
        sectorVol.put("Energy", 0.016);
        sectorVol.put("Consumer Goods", 0.010);
        sectorVol.put("Manufacturing", 0.014);
        sectorVol.put("Telecommunications", 0.012);
        sectorVol.put("Utilities", 0.008);
        sectorVol.put("Index", 0.007);
        sectorVol.put("Government", 0.003);
        sectorVol.put("Corporate", 0.004);
        sectorVol.put("Infrastructure", 0.004);
        sectorVol.put("Commodities", 0.014);
        sectorVol.put("Benchmark", 0.007);

        // ── Market regime state ────────────────────────────────────────────────
        // Regimes: BULL, BEAR, CORRECTION, RECOVERY, SIDEWAYS
        String[] regimes = { "BULL", "BULL", "BULL", "BULL", // 40% bull
                "BEAR", "BEAR", "BEAR", // ~15% bear
                "CORRECTION", "CORRECTION", "CORRECTION", // 15%
                "RECOVERY", "RECOVERY", "RECOVERY", // 15%
                "SIDEWAYS", "SIDEWAYS", "SIDEWAYS" }; // 15%

        // ── Per-asset starting prices (work backwards from current) ───────────
        Map<UUID, BigDecimal> prices = new HashMap<>();
        for (Asset a : allAssets) {
            BigDecimal p = a.getCurrentPrice();
            if (p == null || p.compareTo(BigDecimal.ZERO) <= 0)
                p = BigDecimal.valueOf(100.0);
            // Start from a slightly lower price 5 years ago (simulate growth)
            double startFactor = 0.60 + random.nextDouble() * 0.30; // 60–90% of current
            prices.put(a.getId(), p.multiply(BigDecimal.valueOf(startFactor))
                    .setScale(4, RoundingMode.HALF_UP));
        }

        // ── Batch insert ──────────────────────────────────────────────────────
        List<AssetPriceHistory> batch = new ArrayList<>();
        int batchSize = 500;

        String currentRegime = "BULL";
        int regimeDaysLeft = 30 + random.nextInt(30);

        for (LocalDate date : tradingDays) {

            // Rotate regime
            if (regimeDaysLeft <= 0) {
                currentRegime = regimes[random.nextInt(regimes.length)];
                regimeDaysLeft = 20 + random.nextInt(40);
            }
            regimeDaysLeft--;

            // Market-wide factor based on regime
            double marketDrift = getRegimeDrift(currentRegime);
            double marketNoise = (random.nextGaussian() * 0.005); // small noise
            double marketFactor = 1.0 + marketDrift + marketNoise;

            // Sector factors for the day
            Map<String, Double> sectorFactors = new HashMap<>();
            for (String sector : sectorVol.keySet()) {
                double sectorNoise = random.nextGaussian() * sectorVol.getOrDefault(sector, 0.01);
                sectorFactors.put(sector, 1.0 + sectorNoise);
            }

            for (Asset asset : allAssets) {
                BigDecimal prevPrice = prices.get(asset.getId());

                double vol = getVolatilityForAssetType(asset) * 0.5;
                double assetNoise = random.nextGaussian() * vol;
                double sectorFactor = sectorFactors.getOrDefault(asset.getSector(), 1.0);

                // Bonds and benchmarks not affected by market crash as much
                double effectiveMarket = isDefensive(asset)
                        ? 1.0 + (marketFactor - 1.0) * 0.3
                        : marketFactor;

                double multiplier = effectiveMarket * sectorFactor * (1.0 + assetNoise);

                BigDecimal newPrice = prevPrice.multiply(BigDecimal.valueOf(multiplier))
                        .setScale(4, RoundingMode.HALF_UP);

                // Floor price
                BigDecimal floor = prevPrice.multiply(BigDecimal.valueOf(0.005))
                        .setScale(4, RoundingMode.HALF_UP);
                if (newPrice.compareTo(floor) < 0)
                    newPrice = floor;

                prices.put(asset.getId(), newPrice);

                batch.add(AssetPriceHistory.builder()
                        .assetId(asset.getId())
                        .recordDate(date)
                        .closingPrice(newPrice)
                        .build());

                if (batch.size() >= batchSize) {
                    assetPriceHistoryRepository.saveAll(batch);
                    batch.clear();
                }
            }
        }

        // Flush remaining
        if (!batch.isEmpty()) {
            assetPriceHistoryRepository.saveAll(batch);
        }

        // Update current prices to end of simulation
        for (Asset asset : allAssets) {
            asset.setCurrentPrice(prices.get(asset.getId()));
            assetRepository.save(asset);
        }

        long total = assetPriceHistoryRepository.count();
        log.info("Historical price generation complete. Total records: {}", total);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private double getRegimeDrift(String regime) {
        return switch (regime) {
            case "BULL" -> 0.0008; // +0.08% per day
            case "BEAR" -> -0.0006; // -0.06% per day
            case "CORRECTION" -> -0.0015; // -0.15% per day (sharper drop)
            case "RECOVERY" -> 0.0010; // +0.10% per day
            case "SIDEWAYS" -> 0.0001; // near flat
            default -> 0.0003;
        };
    }

    private boolean isDefensive(Asset asset) {
        if (asset.getAssetType() == null)
            return false;
        return asset.getAssetType() == AssetType.BOND
                || asset.getAssetType() == AssetType.BENCHMARK
                || asset.getAssetType() == AssetType.ETF;
    }

    private double getVolatilityForAssetType(Asset asset) {
        if (asset.getAssetType() == null)
            return 0.02;
        return switch (asset.getAssetType()) {
            case STOCK -> 0.05;
            case ETF -> 0.02;
            case BOND -> 0.005;
            case BENCHMARK -> 0.015;
        };
    }
}