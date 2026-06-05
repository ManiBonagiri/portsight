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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService Tests")
class AnalyticsServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private HoldingRepository holdingRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private PortfolioSnapshotRepository snapshotRepository;
    @Mock
    private AssetPriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    // Shared fixtures
    private UUID userId;
    private UUID portfolioId;
    private UUID assetId;
    private Portfolio portfolio;
    private Asset asset;
    private Holding holding;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();
        assetId = UUID.randomUUID();

        portfolio = new Portfolio();
        portfolio.setId(portfolioId);
        portfolio.setUserId(userId);

        asset = new Asset();
        asset.setId(assetId);
        asset.setCurrentPrice(new BigDecimal("150.00"));

        // 100 shares @ avg 100, currentPrice 150
        holding = Holding.builder()
                .id(UUID.randomUUID())
                .portfolioId(portfolioId)
                .assetId(assetId)
                .quantity(new BigDecimal("100"))
                .averagePrice(new BigDecimal("100.00"))
                .build();
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("generateDailySnapshot")
    class GenerateDailySnapshotTests {

        @Test
        @DisplayName("shouldCreateNewSnapshotWhenNoneExistsForDate")
        void shouldCreateNewSnapshotWhenNoneExistsForDate() {
            LocalDate today = LocalDate.now();

            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(holding));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(snapshotRepository.findByPortfolioIdAndSnapshotDate(portfolioId, today))
                    .thenReturn(Optional.empty());
            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            analyticsService.generateDailySnapshot(portfolioId, today);

            ArgumentCaptor<PortfolioSnapshot> captor = ArgumentCaptor.forClass(PortfolioSnapshot.class);
            verify(snapshotRepository).save(captor.capture());
            PortfolioSnapshot saved = captor.getValue();

            // totalValue = 100 × 150 = 15000
            assertThat(saved.getTotalValue()).isEqualByComparingTo("15000.00");
            // investedAmount = 100 × 100 = 10000
            assertThat(saved.getInvestedAmount()).isEqualByComparingTo("10000.00");
            // unrealizedGain = 5000
            assertThat(saved.getUnrealizedGain()).isEqualByComparingTo("5000.00");
            assertThat(saved.getRealizedGain()).isEqualByComparingTo("0");
            assertThat(saved.getSnapshotDate()).isEqualTo(today);
        }

        @Test
        @DisplayName("shouldUpdateExistingSnapshotWhenOneAlreadyExistsForDate")
        void shouldUpdateExistingSnapshotWhenOneAlreadyExistsForDate() {
            LocalDate today = LocalDate.now();

            PortfolioSnapshot existing = PortfolioSnapshot.builder()
                    .id(UUID.randomUUID())
                    .portfolioId(portfolioId)
                    .snapshotDate(today)
                    .totalValue(new BigDecimal("9000.00"))
                    .investedAmount(new BigDecimal("9000.00"))
                    .unrealizedGain(BigDecimal.ZERO)
                    .realizedGain(BigDecimal.ZERO)
                    .build();

            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(holding));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(snapshotRepository.findByPortfolioIdAndSnapshotDate(portfolioId, today))
                    .thenReturn(Optional.of(existing));
            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            analyticsService.generateDailySnapshot(portfolioId, today);

            ArgumentCaptor<PortfolioSnapshot> captor = ArgumentCaptor.forClass(PortfolioSnapshot.class);
            verify(snapshotRepository).save(captor.capture());
            PortfolioSnapshot saved = captor.getValue();

            // Must be the same object (upsert), updated with new values
            assertThat(saved.getId()).isEqualTo(existing.getId());
            assertThat(saved.getTotalValue()).isEqualByComparingTo("15000.00");
            assertThat(saved.getUnrealizedGain()).isEqualByComparingTo("5000.00");
        }

        @Test
        @DisplayName("shouldSkipSnapshotWhenPortfolioHasNoHoldings")
        void shouldSkipSnapshotWhenPortfolioHasNoHoldings() {
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(Collections.emptyList());

            analyticsService.generateDailySnapshot(portfolioId, LocalDate.now());

            verify(snapshotRepository, never()).save(any());
        }

        @Test
        @DisplayName("shouldFallbackToAveragePriceWhenAssetCurrentPriceIsNull")
        void shouldFallbackToAveragePriceWhenAssetCurrentPriceIsNull() {
            asset.setCurrentPrice(null);
            LocalDate today = LocalDate.now();

            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(holding));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(snapshotRepository.findByPortfolioIdAndSnapshotDate(portfolioId, today))
                    .thenReturn(Optional.empty());
            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            analyticsService.generateDailySnapshot(portfolioId, today);

            ArgumentCaptor<PortfolioSnapshot> captor = ArgumentCaptor.forClass(PortfolioSnapshot.class);
            verify(snapshotRepository).save(captor.capture());
            PortfolioSnapshot saved = captor.getValue();

            // currentPrice null → use avgPrice 100 → totalValue = 100 × 100 = 10000
            assertThat(saved.getTotalValue()).isEqualByComparingTo("10000.00");
            // unrealizedGain = 0 (price == avgPrice)
            assertThat(saved.getUnrealizedGain()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("shouldFallbackToAveragePriceWhenAssetNotFoundInRepository")
        void shouldFallbackToAveragePriceWhenAssetNotFoundInRepository() {
            LocalDate today = LocalDate.now();

            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(holding));
            when(assetRepository.findById(assetId)).thenReturn(Optional.empty()); // asset missing
            when(snapshotRepository.findByPortfolioIdAndSnapshotDate(portfolioId, today))
                    .thenReturn(Optional.empty());
            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            analyticsService.generateDailySnapshot(portfolioId, today);

            ArgumentCaptor<PortfolioSnapshot> captor = ArgumentCaptor.forClass(PortfolioSnapshot.class);
            verify(snapshotRepository).save(captor.capture());
            // Falls back to avgPrice → totalValue = investedAmount = 10000
            assertThat(captor.getValue().getTotalValue()).isEqualByComparingTo("10000.00");
            assertThat(captor.getValue().getUnrealizedGain()).isEqualByComparingTo("0");
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getPortfolioAnalytics")
    class GetPortfolioAnalyticsTests {

        @Test
        @DisplayName("shouldReturnCorrectAnalyticsWithRealPrices")
        void shouldReturnCorrectAnalyticsWithRealPrices() {
            // 100 shares @ avg 100, currentPrice 150
            // portfolioValue = 15000, investedAmount = 10000
            // gainLoss = 5000, returnPercentage = 50.00%
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(holding));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

            AnalyticsResponse result = analyticsService.getPortfolioAnalytics(userId, portfolioId);

            assertThat(result.getPortfolioValue()).isEqualByComparingTo("15000.00");
            assertThat(result.getInvestedAmount()).isEqualByComparingTo("10000.00");
            assertThat(result.getGainLoss()).isEqualByComparingTo("5000.00");
            // (5000 / 10000) × 100 = 50.0000
            assertThat(result.getReturnPercentage()).isEqualByComparingTo("50.0000");
        }

        @Test
        @DisplayName("shouldReturnZeroReturnPercentageWhenInvestedAmountIsZero")
        void shouldReturnZeroReturnPercentageWhenInvestedAmountIsZero() {
            // Holdings with quantity = 0 → investedAmount = 0 → no divide-by-zero
            Holding zeroHolding = Holding.builder()
                    .portfolioId(portfolioId)
                    .assetId(assetId)
                    .quantity(BigDecimal.ZERO)
                    .averagePrice(new BigDecimal("100.00"))
                    .build();

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(zeroHolding));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

            AnalyticsResponse result = analyticsService.getPortfolioAnalytics(userId, portfolioId);

            assertThat(result.getReturnPercentage()).isEqualByComparingTo("0");
            assertThat(result.getPortfolioValue()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("shouldReturnNegativeGainLossWhenCurrentPriceBelowAverage")
        void shouldReturnNegativeGainLossWhenCurrentPriceBelowAverage() {
            // avg 200, currentPrice 150 → loss
            asset.setCurrentPrice(new BigDecimal("150.00"));
            Holding losingHolding = Holding.builder()
                    .portfolioId(portfolioId)
                    .assetId(assetId)
                    .quantity(new BigDecimal("100"))
                    .averagePrice(new BigDecimal("200.00"))
                    .build();

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(losingHolding));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

            AnalyticsResponse result = analyticsService.getPortfolioAnalytics(userId, portfolioId);

            // portfolioValue = 15000, investedAmount = 20000
            assertThat(result.getGainLoss()).isEqualByComparingTo("-5000.00");
            assertThat(result.getReturnPercentage()).isNegative();
        }

        @Test
        @DisplayName("shouldReturnZeroAnalyticsWhenPortfolioHasNoHoldings")
        void shouldReturnZeroAnalyticsWhenPortfolioHasNoHoldings() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(Collections.emptyList());

            AnalyticsResponse result = analyticsService.getPortfolioAnalytics(userId, portfolioId);

            assertThat(result.getPortfolioValue()).isEqualByComparingTo("0");
            assertThat(result.getInvestedAmount()).isEqualByComparingTo("0");
            assertThat(result.getGainLoss()).isEqualByComparingTo("0");
            assertThat(result.getReturnPercentage()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("shouldThrowWhenPortfolioNotFound")
        void shouldThrowWhenPortfolioNotFound() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> analyticsService.getPortfolioAnalytics(userId, portfolioId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(holdingRepository, never()).findByPortfolioId(any());
        }

        @Test
        @DisplayName("shouldThrowWhenUserDoesNotOwnPortfolio")
        void shouldThrowWhenUserDoesNotOwnPortfolio() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> analyticsService.getPortfolioAnalytics(UUID.randomUUID(), portfolioId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(holdingRepository, never()).findByPortfolioId(any());
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getPortfolioPerformance")
    class GetPortfolioPerformanceTests {

        @Test
        @DisplayName("shouldReturnZerosWhenPortfolioHasNoHoldings")
        void shouldReturnZerosWhenPortfolioHasNoHoldings() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(Collections.emptyList());

            PerformanceResponse result = analyticsService.getPortfolioPerformance(userId, portfolioId);

            assertThat(result.getDailyReturn()).isEqualByComparingTo("0");
            assertThat(result.getMonthlyReturn()).isEqualByComparingTo("0");
            assertThat(result.getAnnualReturn()).isEqualByComparingTo("0");
            assertThat(result.getCagr()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("shouldReturnZerosWhenPriceHistoryHasFewerThanFivePoints")
        void shouldReturnZerosWhenPriceHistoryHasFewerThanFivePoints() {
            // Only 3 data points — below the < 5 threshold
            LocalDate base = LocalDate.now().minusDays(10);
            List<AssetPriceHistory> thinHistory = List.of(
                    priceHistory(assetId, base, 100.0),
                    priceHistory(assetId, base.plusDays(1), 101.0),
                    priceHistory(assetId, base.plusDays(2), 102.0));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(holding));
            when(priceHistoryRepository
                    .findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
                            eq(assetId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(thinHistory);

            PerformanceResponse result = analyticsService.getPortfolioPerformance(userId, portfolioId);

            assertThat(result.getDailyReturn()).isEqualByComparingTo("0");
            assertThat(result.getCagr()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("shouldReturnZerosWhenNoPriceHistoryExists")
        void shouldReturnZerosWhenNoPriceHistoryExists() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(holding));
            when(priceHistoryRepository
                    .findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
                            eq(assetId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            PerformanceResponse result = analyticsService.getPortfolioPerformance(userId, portfolioId);

            assertThat(result.getCagr()).isEqualByComparingTo("0");
            assertThat(result.getAnnualReturn()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("shouldCalculatePositiveCAGRWhenPricesGrowOverTime")
        void shouldCalculatePositiveCAGRWhenPricesGrowOverTime() {
            // 10 consecutive trading days of rising prices (> 5 threshold)
            LocalDate base = LocalDate.now().minusDays(20);
            List<AssetPriceHistory> history = buildPriceHistory(assetId, base, 10,
                    100.0, 102.0, 104.0, 106.0, 108.0,
                    110.0, 112.0, 114.0, 116.0, 120.0);

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(holding));
            when(priceHistoryRepository
                    .findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
                            eq(assetId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(history);

            PerformanceResponse result = analyticsService.getPortfolioPerformance(userId, portfolioId);

            // Prices went up 100 → 120, so CAGR must be positive
            assertThat(result.getCagr()).isPositive();
            assertThat(result.getAnnualReturn()).isPositive();
        }

        @Test
        @DisplayName("shouldCalculateNegativeCAGRWhenPricesDeclineOverTime")
        void shouldCalculateNegativeCAGRWhenPricesDeclineOverTime() {
            LocalDate base = LocalDate.now().minusDays(20);
            List<AssetPriceHistory> history = buildPriceHistory(assetId, base, 10,
                    120.0, 118.0, 116.0, 114.0, 112.0,
                    110.0, 108.0, 106.0, 104.0, 100.0);

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(holding));
            when(priceHistoryRepository
                    .findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
                            eq(assetId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(history);

            PerformanceResponse result = analyticsService.getPortfolioPerformance(userId, portfolioId);

            assertThat(result.getCagr()).isNegative();
        }

        @Test
        @DisplayName("shouldCalculateDailyReturnFromLastTwoPricePoints")
        void shouldCalculateDailyReturnFromLastTwoPricePoints() {
            // Last two prices: 100 → 110 → daily return = +10%
            LocalDate base = LocalDate.now().minusDays(20);
            List<AssetPriceHistory> history = buildPriceHistory(assetId, base, 7,
                    100.0, 101.0, 102.0, 103.0, 104.0, 100.0, 110.0);

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(holding));
            when(priceHistoryRepository
                    .findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
                            eq(assetId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(history);

            PerformanceResponse result = analyticsService.getPortfolioPerformance(userId, portfolioId);

            // dailyReturn = (110 - 100) / 100 × 100 = 10%
            assertThat(result.getDailyReturn())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("10.0000");
        }

        @Test
        @DisplayName("shouldThrowWhenPortfolioNotFoundOnPerformance")
        void shouldThrowWhenPortfolioNotFoundOnPerformance() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> analyticsService.getPortfolioPerformance(userId, portfolioId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("shouldThrowWhenUserDoesNotOwnPortfolioOnPerformance")
        void shouldThrowWhenUserDoesNotOwnPortfolioOnPerformance() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> analyticsService.getPortfolioPerformance(UUID.randomUUID(), portfolioId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Build a consecutive-day price history list from explicit price values. */
    private List<AssetPriceHistory> buildPriceHistory(UUID assetId, LocalDate startDate,
            int count, double... prices) {
        List<AssetPriceHistory> result = new java.util.ArrayList<>();
        for (int i = 0; i < count && i < prices.length; i++) {
            result.add(priceHistory(assetId, startDate.plusDays(i), prices[i]));
        }
        return result;
    }

    /** Create a single AssetPriceHistory record. */
    private AssetPriceHistory priceHistory(UUID assetId, LocalDate date, double price) {
        AssetPriceHistory aph = new AssetPriceHistory();
        aph.setAssetId(assetId);
        aph.setRecordDate(date);
        aph.setClosingPrice(BigDecimal.valueOf(price));
        return aph;
    }
}