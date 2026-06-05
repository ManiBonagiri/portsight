package com.portsight.api.domains.portfolio.service;

import com.portsight.api.domains.portfolio.dto.CreatePortfolioRequest;
import com.portsight.api.domains.portfolio.dto.PortfolioResponse;
import com.portsight.api.domains.portfolio.dto.UpdatePortfolioRequest;
import com.portsight.api.domains.portfolio.entity.Portfolio;
import com.portsight.api.domains.portfolio.mapper.PortfolioMapper;
import com.portsight.api.domains.portfolio.repository.PortfolioRepository;
import com.portsight.api.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PortfolioServiceImpl implements PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioServiceImpl.class);

    private final PortfolioRepository portfolioRepository;
    private final PortfolioMapper portfolioMapper;

    public PortfolioServiceImpl(PortfolioRepository portfolioRepository, PortfolioMapper portfolioMapper) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioMapper = portfolioMapper;
    }

    @Override
    @Transactional
    public PortfolioResponse createPortfolio(UUID userId, CreatePortfolioRequest request) {
        log.info("Creating portfolio for user: {}", userId);
        Portfolio portfolio = portfolioMapper.toEntity(request);
        portfolio.setUserId(userId);
        portfolio.setStatus("ACTIVE");

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        // TODO: Publish Kafka event 'portfolio-created'
        return portfolioMapper.toResponse(savedPortfolio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getUserPortfolios(UUID userId) {
        log.info("Fetching portfolios for user: {}", userId);
        return portfolioRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .stream()
                .map(portfolioMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(UUID userId, UUID portfolioId) {
        log.info("Fetching portfolio: {} for user: {}", portfolioId, userId);
        Portfolio portfolio = findPortfolioOrThrow(userId, portfolioId);
        return portfolioMapper.toResponse(portfolio);
    }

    @Override
    @Transactional
    public PortfolioResponse updatePortfolio(UUID userId, UUID portfolioId, UpdatePortfolioRequest request) {
        log.info("Updating portfolio: {} for user: {}", portfolioId, userId);
        Portfolio portfolio = findPortfolioOrThrow(userId, portfolioId);

        if (request.getPortfolioName() != null) {
            portfolio.setPortfolioName(request.getPortfolioName());
        }
        if (request.getRiskProfile() != null) {
            portfolio.setRiskProfile(request.getRiskProfile());
        }
        if (request.getBenchmark() != null) {
            portfolio.setBenchmark(request.getBenchmark());
        }

        Portfolio updatedPortfolio = portfolioRepository.save(portfolio);
        return portfolioMapper.toResponse(updatedPortfolio);
    }

    @Override
    @Transactional
    public void archivePortfolio(UUID userId, UUID portfolioId) {
        log.info("Archiving portfolio: {} for user: {}", portfolioId, userId);
        Portfolio portfolio = findPortfolioOrThrow(userId, portfolioId);
        portfolio.setStatus("ARCHIVED");
        portfolioRepository.save(portfolio);
    }

    private Portfolio findPortfolioOrThrow(UUID userId, UUID portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));

        if (!portfolio.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Portfolio not found: " + portfolioId);
        }
        return portfolio;
    }
}
