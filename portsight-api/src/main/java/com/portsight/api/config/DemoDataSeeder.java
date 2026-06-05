package com.portsight.api.config;

import com.portsight.api.domains.asset.entity.Asset;
import com.portsight.api.domains.asset.repository.AssetRepository;
import com.portsight.api.domains.auth.User;
import com.portsight.api.domains.auth.UserRepository;
import com.portsight.api.domains.holding.entity.Holding;
import com.portsight.api.domains.holding.repository.HoldingRepository;
import com.portsight.api.domains.portfolio.entity.Portfolio;
import com.portsight.api.domains.portfolio.enums.RiskProfile;
import com.portsight.api.domains.portfolio.repository.PortfolioRepository;
import com.portsight.api.domains.transaction.entity.Transaction;
import com.portsight.api.domains.transaction.enums.TransactionType;
import com.portsight.api.domains.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class DemoDataSeeder {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;

    @Bean
    @Order(3)
    public CommandLineRunner demoPortfolioSeeder() {
        return args -> {
            if (portfolioRepository.count() > 0) {
                log.info("Demo portfolios already exist. Skipping.");
                return;
            }

            log.info("Seeding demo portfolios, holdings and transactions...");
            seedInvestorPortfolios();
            seedManagerPortfolios();
            seedAdminPortfolios();
            log.info("Demo seeding complete.");
        };
    }

    // ─── Investor: 1 Conservative Portfolio ──────────────────────────────────
    private void seedInvestorPortfolios() {
        User investor = findUser("investor@portsight.com");
        if (investor == null)
            return;

        Portfolio p = createPortfolio(investor.getId(), "Conservative Growth",
                RiskProfile.CONSERVATIVE, "SIM_NIFTY");

        addHoldingAndTx(p, "TECH01", 150, new BigDecimal("1420.00")); // Infosys
        addHoldingAndTx(p, "BANK01", 80, new BigDecimal("1580.00")); // HDFC Bank
        addHoldingAndTx(p, "BANK02", 100, new BigDecimal("980.00")); // ICICI Bank
        addHoldingAndTx(p, "HLTH01", 60, new BigDecimal("1180.00")); // Sun Pharma
        addHoldingAndTx(p, "TECH02", 50, new BigDecimal("3650.00")); // TCS
        addHoldingAndTx(p, "TECH03", 200, new BigDecimal("470.00")); // Wipro
        addHoldingAndTx(p, "HLTH02", 30, new BigDecimal("5900.00")); // Dr Reddys
        addHoldingAndTx(p, "CONS02", 500, new BigDecimal("438.00")); // ITC
        addHoldingAndTx(p, "ETF01", 100, new BigDecimal("220.00")); // Nifty50 ETF
        addHoldingAndTx(p, "ETF09", 50, new BigDecimal("5480.00")); // Gold Bees

        log.info("Seeded investor portfolio: {}", p.getPortfolioName());
    }

    // ─── Manager: 2 Portfolios ────────────────────────────────────────────────
    private void seedManagerPortfolios() {
        User manager = findUser("manager@portsight.com");
        if (manager == null)
            return;

        Portfolio p1 = createPortfolio(manager.getId(), "Aggressive Growth",
                RiskProfile.AGGRESSIVE, "SIM_NIFTY");

        addHoldingAndTx(p1, "TECH01", 200, new BigDecimal("1390.00")); // Infosys
        addHoldingAndTx(p1, "TECH02", 100, new BigDecimal("3580.00")); // TCS
        addHoldingAndTx(p1, "TECH04", 150, new BigDecimal("1520.00")); // HCL Tech
        addHoldingAndTx(p1, "TECH07", 50, new BigDecimal("4800.00")); // Persistent
        addHoldingAndTx(p1, "ENRG01", 80, new BigDecimal("2920.00")); // Reliance
        addHoldingAndTx(p1, "MFGR01", 200, new BigDecimal("920.00")); // Tata Motors
        addHoldingAndTx(p1, "ENRG06", 100, new BigDecimal("1680.00")); // Adani Green
        addHoldingAndTx(p1, "BANK05", 120, new BigDecimal("1050.00")); // Axis Bank

        log.info("Seeded manager portfolio 1: {}", p1.getPortfolioName());

        Portfolio p2 = createPortfolio(manager.getId(), "Balanced Core",
                RiskProfile.MODERATE, "SIM_SENSEX");

        addHoldingAndTx(p2, "BANK01", 100, new BigDecimal("1560.00")); // HDFC Bank
        addHoldingAndTx(p2, "BANK02", 120, new BigDecimal("970.00")); // ICICI Bank
        addHoldingAndTx(p2, "BANK03", 300, new BigDecimal("760.00")); // SBI
        addHoldingAndTx(p2, "HLTH01", 80, new BigDecimal("1160.00")); // Sun Pharma
        addHoldingAndTx(p2, "HLTH03", 100, new BigDecimal("1420.00")); // Cipla
        addHoldingAndTx(p2, "CONS01", 60, new BigDecimal("2650.00")); // HUL
        addHoldingAndTx(p2, "CONS03", 20, new BigDecimal("24500.00")); // Nestle
        addHoldingAndTx(p2, "ETF04", 200, new BigDecimal("480.00")); // Bank ETF
        addHoldingAndTx(p2, "BND01", 50, new BigDecimal("1020.00")); // GOI 10Y
        addHoldingAndTx(p2, "ETF08", 100, new BigDecimal("1285.00")); // Bharat Bond

        log.info("Seeded manager portfolio 2: {}", p2.getPortfolioName());
    }

    // ─── Admin: 3 Portfolios ──────────────────────────────────────────────────
    private void seedAdminPortfolios() {
        User admin = findUser("admin@portsight.com");
        if (admin == null)
            return;

        Portfolio p1 = createPortfolio(admin.getId(), "Tech Sector Focus",
                RiskProfile.AGGRESSIVE, "SIM_NIFTY");

        addHoldingAndTx(p1, "TECH01", 300, new BigDecimal("1380.00")); // Infosys
        addHoldingAndTx(p1, "TECH02", 150, new BigDecimal("3520.00")); // TCS
        addHoldingAndTx(p1, "TECH03", 400, new BigDecimal("460.00")); // Wipro
        addHoldingAndTx(p1, "TECH04", 200, new BigDecimal("1500.00")); // HCL Tech
        addHoldingAndTx(p1, "TECH05", 180, new BigDecimal("1620.00")); // Tech Mahindra
        addHoldingAndTx(p1, "TECH06", 100, new BigDecimal("2450.00")); // Mphasis
        addHoldingAndTx(p1, "TECH07", 80, new BigDecimal("4750.00")); // Persistent
        addHoldingAndTx(p1, "TECH08", 60, new BigDecimal("7200.00")); // Coforge

        log.info("Seeded admin portfolio 1: {}", p1.getPortfolioName());

        Portfolio p2 = createPortfolio(admin.getId(), "Dividend Income",
                RiskProfile.CONSERVATIVE, "SIM_SENSEX");

        addHoldingAndTx(p2, "CONS02", 800, new BigDecimal("432.00")); // ITC
        addHoldingAndTx(p2, "ENRG03", 400, new BigDecimal("445.00")); // Coal India
        addHoldingAndTx(p2, "ENRG04", 500, new BigDecimal("338.00")); // NTPC
        addHoldingAndTx(p2, "ENRG05", 600, new BigDecimal("292.00")); // Power Grid
        addHoldingAndTx(p2, "ENRG02", 300, new BigDecimal("268.00")); // ONGC
        addHoldingAndTx(p2, "ETF09", 200, new BigDecimal("5480.00")); // Gold Bees
        addHoldingAndTx(p2, "BND02", 100, new BigDecimal("1000.00")); // GOI 5Y
        addHoldingAndTx(p2, "BND03", 150, new BigDecimal("1025.00")); // SBI Corp Bond

        log.info("Seeded admin portfolio 2: {}", p2.getPortfolioName());

        Portfolio p3 = createPortfolio(admin.getId(), "All Weather",
                RiskProfile.MODERATE, "SIM_NIFTY");

        addHoldingAndTx(p3, "ENRG01", 100, new BigDecimal("2900.00")); // Reliance
        addHoldingAndTx(p3, "BANK01", 150, new BigDecimal("1550.00")); // HDFC Bank
        addHoldingAndTx(p3, "TECH01", 200, new BigDecimal("1400.00")); // Infosys
        addHoldingAndTx(p3, "HLTH01", 100, new BigDecimal("1150.00")); // Sun Pharma
        addHoldingAndTx(p3, "MFGR02", 20, new BigDecimal("12500.00")); // Maruti
        addHoldingAndTx(p3, "BANK04", 30, new BigDecimal("1820.00")); // Kotak Bank
        addHoldingAndTx(p3, "ETF01", 500, new BigDecimal("215.00")); // Nifty50 ETF
        addHoldingAndTx(p3, "ETF09", 300, new BigDecimal("5480.00")); // Gold Bees
        addHoldingAndTx(p3, "ETF08", 200, new BigDecimal("1285.00")); // Bharat Bond
        addHoldingAndTx(p3, "BND01", 80, new BigDecimal("1000.00")); // GOI 10Y

        log.info("Seeded admin portfolio 3: {}", p3.getPortfolioName());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private User findUser(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    private Portfolio createPortfolio(UUID userId, String name,
            RiskProfile riskProfile, String benchmark) {
        Portfolio p = new Portfolio();
        p.setUserId(userId);
        p.setPortfolioName(name);
        p.setRiskProfile(riskProfile);
        p.setBenchmark(benchmark);
        p.setStatus("ACTIVE");
        return portfolioRepository.save(p);
    }

    private void addHoldingAndTx(Portfolio portfolio, String ticker,
            int qty, BigDecimal price) {
        Optional<Asset> assetOpt = assetRepository
                .searchAssets(ticker, null, null)
                .stream().findFirst();

        if (assetOpt.isEmpty()) {
            log.warn("Asset not found for ticker: {} — skipping", ticker);
            return;
        }

        Asset asset = assetOpt.get();
        BigDecimal quantity = BigDecimal.valueOf(qty);

        Holding holding = holdingRepository
                .findByPortfolioIdAndAssetId(portfolio.getId(), asset.getId())
                .orElse(Holding.builder()
                        .portfolioId(portfolio.getId())
                        .assetId(asset.getId())
                        .quantity(BigDecimal.ZERO)
                        .averagePrice(BigDecimal.ZERO)
                        .build());

        BigDecimal oldTotal = holding.getQuantity().multiply(holding.getAveragePrice());
        BigDecimal newTotal = quantity.multiply(price);
        BigDecimal totalQty = holding.getQuantity().add(quantity);
        BigDecimal newAvgPrice = oldTotal.add(newTotal)
                .divide(totalQty, 4, RoundingMode.HALF_UP);

        holding.setQuantity(totalQty);
        holding.setAveragePrice(newAvgPrice);
        holdingRepository.save(holding);

        Transaction tx = new Transaction();
        tx.setPortfolioId(portfolio.getId());
        tx.setAssetId(asset.getId());
        tx.setType(TransactionType.BUY);
        tx.setQuantity(quantity);
        tx.setPrice(price);
        tx.setStatus("COMPLETED");
        transactionRepository.save(tx);
    }
}