package com.portsight.api.domains.portfolio.service;

import com.portsight.api.domains.portfolio.dto.CreatePortfolioRequest;
import com.portsight.api.domains.portfolio.dto.PortfolioResponse;
import com.portsight.api.domains.portfolio.dto.UpdatePortfolioRequest;

import java.util.List;
import java.util.UUID;

public interface PortfolioService {
    PortfolioResponse createPortfolio(UUID userId, CreatePortfolioRequest request);
    List<PortfolioResponse> getUserPortfolios(UUID userId);
    PortfolioResponse getPortfolio(UUID userId, UUID portfolioId);
    PortfolioResponse updatePortfolio(UUID userId, UUID portfolioId, UpdatePortfolioRequest request);
    void archivePortfolio(UUID userId, UUID portfolioId);
}
