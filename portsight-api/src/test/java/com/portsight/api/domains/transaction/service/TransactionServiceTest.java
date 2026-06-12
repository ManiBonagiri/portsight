package com.portsight.api.domains.transaction.service;

import com.portsight.api.domains.asset.dto.AssetResponse;
import com.portsight.api.domains.asset.entity.Asset;
import com.portsight.api.domains.asset.mapper.AssetMapper;
import com.portsight.api.domains.asset.repository.AssetRepository;
import com.portsight.api.domains.portfolio.entity.Portfolio;
import com.portsight.api.domains.portfolio.enums.RiskProfile;
import com.portsight.api.domains.portfolio.repository.PortfolioRepository;
import com.portsight.api.domains.transaction.dto.TransactionRequest;
import com.portsight.api.domains.transaction.dto.TransactionResponse;
import com.portsight.api.domains.transaction.entity.Transaction;
import com.portsight.api.domains.transaction.enums.TransactionType;
import com.portsight.api.domains.transaction.mapper.TransactionMapper;
import com.portsight.api.domains.transaction.repository.TransactionRepository;
import com.portsight.api.shared.exception.ResourceNotFoundException;
import com.portsight.api.shared.outbox.OutboxEventHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AssetMapper assetMapper;
    @Mock
    private OutboxEventHelper outboxEventHelper;

    @InjectMocks
    private TransactionService transactionService;

    private UUID userId;
    private UUID portfolioId;
    private UUID assetId;
    private UUID transactionId;
    private Portfolio portfolio;
    private Transaction transaction;
    private TransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();
        assetId = UUID.randomUUID();
        transactionId = UUID.randomUUID();

        portfolio = Portfolio.builder()
                .id(portfolioId)
                .userId(userId)
                .portfolioName("Test Portfolio")
                .riskProfile(RiskProfile.MODERATE)
                .benchmark("SIM_NIFTY")
                .status("ACTIVE")
                .build();

        transaction = Transaction.builder()
                .id(transactionId)
                .portfolioId(portfolioId)
                .assetId(assetId)
                .type(TransactionType.BUY)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("150.00"))
                .status("COMPLETED")
                .build();

        transactionResponse = new TransactionResponse();
        transactionResponse.setId(transactionId);
        transactionResponse.setPortfolioId(portfolioId);
        transactionResponse.setAssetId(assetId);
        transactionResponse.setType(TransactionType.BUY);
        transactionResponse.setQuantity(new BigDecimal("100"));
        transactionResponse.setPrice(new BigDecimal("150.00"));
        transactionResponse.setStatus("COMPLETED");
    }

    // -----------------------------------------------------------------------
    // RECORD TRANSACTION
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("recordTransaction")
    class RecordTransactionTests {

        @Test
        @DisplayName("shouldRecordBuyTransactionSuccessfully")
        void shouldRecordBuyTransactionSuccessfully() {
            TransactionRequest request = new TransactionRequest(
                    portfolioId, assetId, TransactionType.BUY,
                    new BigDecimal("100"), new BigDecimal("150.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(new Asset()));
            when(transactionMapper.toEntity(request)).thenReturn(transaction);
            when(transactionRepository.save(transaction)).thenReturn(transaction);
            when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(new Asset()));
            when(assetMapper.toResponse(any(Asset.class))).thenReturn(new AssetResponse());

            TransactionResponse result = transactionService.recordTransaction(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(TransactionType.BUY);
            verify(transactionRepository).save(transaction);
        }

        @Test
        @DisplayName("shouldSetTotalAmountAsQuantityTimesPrice")
        void shouldSetTotalAmountAsQuantityTimesPrice() {
            TransactionRequest request = new TransactionRequest(
                    portfolioId, assetId, TransactionType.BUY,
                    new BigDecimal("100"), new BigDecimal("150.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(new Asset()));
            when(transactionMapper.toEntity(request)).thenReturn(transaction);
            when(transactionRepository.save(transaction)).thenReturn(transaction);
            when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);
            when(assetMapper.toResponse(any(Asset.class))).thenReturn(new AssetResponse());

            TransactionResponse result = transactionService.recordTransaction(userId, request);

            // 100 * 150.00 = 15000.00
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("15000.00"));
        }

        @Test
        @DisplayName("shouldThrowWhenPortfolioNotFoundOnRecord")
        void shouldThrowWhenPortfolioNotFoundOnRecord() {
            TransactionRequest request = new TransactionRequest(
                    portfolioId, assetId, TransactionType.BUY,
                    new BigDecimal("100"), new BigDecimal("150.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.recordTransaction(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("shouldThrowWhenUserDoesNotOwnPortfolioOnRecord")
        void shouldThrowWhenUserDoesNotOwnPortfolioOnRecord() {
            UUID differentUserId = UUID.randomUUID();
            TransactionRequest request = new TransactionRequest(
                    portfolioId, assetId, TransactionType.BUY,
                    new BigDecimal("100"), new BigDecimal("150.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> transactionService.recordTransaction(differentUserId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("shouldThrowWhenAssetNotFound")
        void shouldThrowWhenAssetNotFound() {
            TransactionRequest request = new TransactionRequest(
                    portfolioId, assetId, TransactionType.BUY,
                    new BigDecimal("100"), new BigDecimal("150.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.recordTransaction(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(assetId.toString());

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("shouldAllowTransactionWithNoAssetForCashDeposit")
        void shouldAllowTransactionWithNoAssetForCashDeposit() {
            TransactionRequest request = new TransactionRequest(
                    portfolioId, null, TransactionType.DEPOSIT,
                    new BigDecimal("500000"), new BigDecimal("1.00"));

            Transaction cashTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .portfolioId(portfolioId)
                    .assetId(null)
                    .type(TransactionType.DEPOSIT)
                    .quantity(new BigDecimal("500000"))
                    .price(new BigDecimal("1.00"))
                    .status("COMPLETED")
                    .build();

            TransactionResponse cashResponse = new TransactionResponse();
            cashResponse.setQuantity(new BigDecimal("500000"));
            cashResponse.setPrice(new BigDecimal("1.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(transactionMapper.toEntity(request)).thenReturn(cashTransaction);
            when(transactionRepository.save(cashTransaction)).thenReturn(cashTransaction);
            when(transactionMapper.toResponse(cashTransaction)).thenReturn(cashResponse);

            TransactionResponse result = transactionService.recordTransaction(userId, request);

            assertThat(result).isNotNull();
            verify(assetRepository, never()).findById(any()); // no asset lookup for null assetId
        }
    }

    // -----------------------------------------------------------------------
    // GET TRANSACTIONS
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getPortfolioTransactions")
    class GetPortfolioTransactionsTests {

        @Test
        @DisplayName("shouldReturnTransactionsForPortfolio")
        void shouldReturnTransactionsForPortfolio() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioId))
                    .thenReturn(List.of(transaction));
            when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(new Asset()));
            when(assetMapper.toResponse(any(Asset.class))).thenReturn(new AssetResponse());

            List<TransactionResponse> result = transactionService.getPortfolioTransactions(userId, portfolioId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(TransactionType.BUY);
        }

        @Test
        @DisplayName("shouldReturnEmptyListWhenNoTransactions")
        void shouldReturnEmptyListWhenNoTransactions() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioId))
                    .thenReturn(List.of());

            List<TransactionResponse> result = transactionService.getPortfolioTransactions(userId, portfolioId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("shouldThrowWhenUnauthorizedUserFetchesTransactions")
        void shouldThrowWhenUnauthorizedUserFetchesTransactions() {
            UUID differentUserId = UUID.randomUUID();
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> transactionService.getPortfolioTransactions(differentUserId, portfolioId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // REVERSE TRANSACTION
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("reverseTransaction")
    class ReverseTransactionTests {

        @Test
        @DisplayName("shouldReverseCompletedTransactionSuccessfully")
        void shouldReverseCompletedTransactionSuccessfully() {
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(transactionRepository.save(transaction)).thenReturn(transaction);
            when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(new Asset()));
            when(assetMapper.toResponse(any(Asset.class))).thenReturn(new AssetResponse());

            transactionService.reverseTransaction(userId, transactionId);

            assertThat(transaction.getStatus()).isEqualTo("REVERSED");
            verify(transactionRepository).save(transaction);
        }

        @Test
        @DisplayName("shouldThrowWhenTransactionNotFound")
        void shouldThrowWhenTransactionNotFound() {
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.reverseTransaction(userId, transactionId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(transactionId.toString());
        }

        @Test
        @DisplayName("shouldThrowWhenTransactionAlreadyReversed")
        void shouldThrowWhenTransactionAlreadyReversed() {
            transaction.setStatus("REVERSED");
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> transactionService.reverseTransaction(userId, transactionId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already reversed");
        }

        @Test
        @DisplayName("shouldThrowWhenUnauthorizedUserReversesTransaction")
        void shouldThrowWhenUnauthorizedUserReversesTransaction() {
            UUID differentUserId = UUID.randomUUID();
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> transactionService.reverseTransaction(differentUserId, transactionId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(transactionRepository, never()).save(any());
        }
    }
}