package com.portsight.api.domains.market.config;

import com.portsight.api.domains.asset.entity.Asset;
import com.portsight.api.domains.asset.enums.AssetType;
import com.portsight.api.domains.asset.repository.AssetRepository;
import com.portsight.api.domains.market.repository.AssetPriceHistoryRepository;
import com.portsight.api.domains.market.service.MarketSimulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final long EXPECTED_ASSET_COUNT = 68;

    private final AssetRepository assetRepository;
    private final AssetPriceHistoryRepository assetPriceHistoryRepository;
    private final MarketSimulationService marketSimulationService;

    public DataSeeder(AssetRepository assetRepository,
            AssetPriceHistoryRepository assetPriceHistoryRepository,
            MarketSimulationService marketSimulationService) {
        this.assetRepository = assetRepository;
        this.assetPriceHistoryRepository = assetPriceHistoryRepository;
        this.marketSimulationService = marketSimulationService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // ── Step 1: Seed assets ──────────────────────────────────────────────
        long count = assetRepository.count();
        if (count < EXPECTED_ASSET_COUNT) {
            log.info("Asset count ({}) is below expected ({}). Reseeding market data...", count, EXPECTED_ASSET_COUNT);
            assetRepository.deleteAll();
            seedAssets();
        } else {
            log.info("Market data already seeded ({} assets). Skipping.", count);
        }

        // ── Step 2: Generate price history if empty ──────────────────────────
        long historyCount = assetPriceHistoryRepository.count();
        if (historyCount == 0) {
            log.info("asset_price_history is empty. Generating 5-year historical prices...");
            marketSimulationService.generateHistoricalPrices();
        } else {
            log.info("Price history already exists ({} records). Skipping generation.", historyCount);
        }
    }

    private void seedAssets() {
        List<Asset> assets = Arrays.asList(

                // ── TECHNOLOGY (10 stocks) ────────────────────────────────────────
                createAsset("TECH01", "Infosys Ltd", "Technology", AssetType.STOCK, "1425.00"),
                createAsset("TECH02", "Tata Consultancy Services", "Technology", AssetType.STOCK, "3890.00"),
                createAsset("TECH03", "Wipro Ltd", "Technology", AssetType.STOCK, "452.00"),
                createAsset("TECH04", "HCL Technologies", "Technology", AssetType.STOCK, "1320.00"),
                createAsset("TECH05", "Tech Mahindra", "Technology", AssetType.STOCK, "1185.00"),
                createAsset("TECH06", "Mphasis Ltd", "Technology", AssetType.STOCK, "2240.00"),
                createAsset("TECH07", "Persistent Systems", "Technology", AssetType.STOCK, "4750.00"),
                createAsset("TECH08", "Coforge Ltd", "Technology", AssetType.STOCK, "5980.00"),
                createAsset("TECH09", "LTIMindtree", "Technology", AssetType.STOCK, "5120.00"),
                createAsset("TECH10", "Zensar Technologies", "Technology", AssetType.STOCK, "620.00"),

                // ── BANKING & FINANCIALS (8 stocks) ──────────────────────────────
                createAsset("BANK01", "HDFC Bank Ltd", "Banking", AssetType.STOCK, "1580.00"),
                createAsset("BANK02", "ICICI Bank Ltd", "Banking", AssetType.STOCK, "1045.00"),
                createAsset("BANK03", "State Bank of India", "Banking", AssetType.STOCK, "780.00"),
                createAsset("BANK04", "Kotak Mahindra Bank", "Banking", AssetType.STOCK, "1820.00"),
                createAsset("BANK05", "Axis Bank Ltd", "Banking", AssetType.STOCK, "1125.00"),
                createAsset("BANK06", "IndusInd Bank", "Banking", AssetType.STOCK, "1430.00"),
                createAsset("BANK07", "Punjab National Bank", "Banking", AssetType.STOCK, "112.00"),
                createAsset("BANK08", "Bank of Baroda", "Banking", AssetType.STOCK, "248.00"),

                // ── HEALTHCARE & PHARMA (7 stocks) ───────────────────────────────
                createAsset("HLTH01", "Sun Pharmaceutical", "Healthcare", AssetType.STOCK, "1620.00"),
                createAsset("HLTH02", "Dr Reddys Laboratories", "Healthcare", AssetType.STOCK, "6240.00"),
                createAsset("HLTH03", "Cipla Ltd", "Healthcare", AssetType.STOCK, "1380.00"),
                createAsset("HLTH04", "Divi's Laboratories", "Healthcare", AssetType.STOCK, "3850.00"),
                createAsset("HLTH05", "Apollo Hospitals", "Healthcare", AssetType.STOCK, "6920.00"),
                createAsset("HLTH06", "Lupin Ltd", "Healthcare", AssetType.STOCK, "1580.00"),
                createAsset("HLTH07", "Aurobindo Pharma", "Healthcare", AssetType.STOCK, "985.00"),

                // ── ENERGY (6 stocks) ─────────────────────────────────────────────
                createAsset("ENRG01", "Reliance Industries", "Energy", AssetType.STOCK, "2890.00"),
                createAsset("ENRG02", "ONGC Ltd", "Energy", AssetType.STOCK, "278.00"),
                createAsset("ENRG03", "Coal India Ltd", "Energy", AssetType.STOCK, "465.00"),
                createAsset("ENRG04", "NTPC Ltd", "Energy", AssetType.STOCK, "358.00"),
                createAsset("ENRG05", "Power Grid Corporation", "Energy", AssetType.STOCK, "312.00"),
                createAsset("ENRG06", "Adani Green Energy", "Energy", AssetType.STOCK, "1820.00"),

                // ── CONSUMER GOODS (7 stocks) ─────────────────────────────────────
                createAsset("CONS01", "Hindustan Unilever", "Consumer Goods", AssetType.STOCK, "2580.00"),
                createAsset("CONS02", "ITC Ltd", "Consumer Goods", AssetType.STOCK, "458.00"),
                createAsset("CONS03", "Nestle India", "Consumer Goods", AssetType.STOCK, "24500.00"),
                createAsset("CONS04", "Britannia Industries", "Consumer Goods", AssetType.STOCK, "5120.00"),
                createAsset("CONS05", "Dabur India", "Consumer Goods", AssetType.STOCK, "545.00"),
                createAsset("CONS06", "Marico Ltd", "Consumer Goods", AssetType.STOCK, "620.00"),
                createAsset("CONS07", "Godrej Consumer Products", "Consumer Goods", AssetType.STOCK, "1280.00"),

                // ── MANUFACTURING & AUTO (6 stocks) ──────────────────────────────
                createAsset("MFGR01", "Tata Motors Ltd", "Manufacturing", AssetType.STOCK, "945.00"),
                createAsset("MFGR02", "Maruti Suzuki India", "Manufacturing", AssetType.STOCK, "12450.00"),
                createAsset("MFGR03", "Mahindra and Mahindra", "Manufacturing", AssetType.STOCK, "1920.00"),
                createAsset("MFGR04", "Larsen and Toubro", "Manufacturing", AssetType.STOCK, "3580.00"),
                createAsset("MFGR05", "Bharat Forge", "Manufacturing", AssetType.STOCK, "1245.00"),
                createAsset("MFGR06", "Bajaj Auto Ltd", "Manufacturing", AssetType.STOCK, "8950.00"),

                // ── TELECOMMUNICATIONS (3 stocks) ────────────────────────────────
                createAsset("TELC01", "Bharti Airtel", "Telecommunications", AssetType.STOCK, "1285.00"),
                createAsset("TELC02", "Vodafone Idea", "Telecommunications", AssetType.STOCK, "14.50"),
                createAsset("TELC03", "Tata Communications", "Telecommunications", AssetType.STOCK, "1820.00"),

                // ── UTILITIES (3 stocks) ──────────────────────────────────────────
                createAsset("UTIL01", "Tata Power Company", "Utilities", AssetType.STOCK, "425.00"),
                createAsset("UTIL02", "Adani Transmission", "Utilities", AssetType.STOCK, "890.00"),
                createAsset("UTIL03", "CESC Ltd", "Utilities", AssetType.STOCK, "185.00"),

                // ── ETFs (10) ─────────────────────────────────────────────────────
                createAsset("ETF01", "Nippon India Nifty 50 ETF", "Index", AssetType.ETF, "215.00"),
                createAsset("ETF02", "SBI Nifty 50 ETF", "Index", AssetType.ETF, "218.00"),
                createAsset("ETF03", "HDFC Nifty 50 ETF", "Index", AssetType.ETF, "220.00"),
                createAsset("ETF04", "Mirae Asset Nifty Bank ETF", "Banking", AssetType.ETF, "425.00"),
                createAsset("ETF05", "Kotak IT ETF", "Technology", AssetType.ETF, "185.00"),
                createAsset("ETF06", "Nippon India Pharma ETF", "Healthcare", AssetType.ETF, "245.00"),
                createAsset("ETF07", "ICICI Pru Consumption ETF", "Consumer Goods", AssetType.ETF, "95.00"),
                createAsset("ETF08", "Bharat Bond ETF", "Government", AssetType.ETF, "1285.00"),
                createAsset("ETF09", "Gold Bees ETF", "Commodities", AssetType.ETF, "5480.00"),
                createAsset("ETF10", "Nifty Next 50 ETF", "Index", AssetType.ETF, "680.00"),

                // ── BONDS (5) ─────────────────────────────────────────────────────
                createAsset("BND01", "GOI 10Y Bond 2034", "Government", AssetType.BOND, "1000.00"),
                createAsset("BND02", "GOI 5Y Bond 2029", "Government", AssetType.BOND, "1000.00"),
                createAsset("BND03", "SBI Corporate Bond", "Corporate", AssetType.BOND, "1025.00"),
                createAsset("BND04", "HDFC Corporate Bond", "Corporate", AssetType.BOND, "1018.00"),
                createAsset("BND05", "REC Infrastructure Bond", "Infrastructure", AssetType.BOND, "1032.00"),

                // ── BENCHMARKS (3) ────────────────────────────────────────────────
                createAsset("SIM_NIFTY", "Simulated Nifty 50", "Benchmark", AssetType.BENCHMARK, "22450.00"),
                createAsset("SIM_SENSEX", "Simulated Sensex", "Benchmark", AssetType.BENCHMARK, "73800.00"),
                createAsset("SIM_BALANCED", "Simulated Balanced Index", "Benchmark", AssetType.BENCHMARK, "15200.00"));

        assetRepository.saveAll(assets);
        log.info("Successfully seeded {} assets in INR (₹).", assets.size());
    }

    private Asset createAsset(String ticker, String name, String sector,
            AssetType type, String price) {
        return Asset.builder()
                .ticker(ticker)
                .name(name)
                .sector(sector)
                .assetType(type)
                .currentPrice(new BigDecimal(price))
                .build();
    }
}