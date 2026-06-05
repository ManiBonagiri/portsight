package com.portsight.api.domains.asset.service;

import com.portsight.api.domains.asset.dto.AssetResponse;
import com.portsight.api.domains.asset.enums.AssetType;
import com.portsight.api.domains.asset.mapper.AssetMapper;
import com.portsight.api.domains.asset.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetMapper assetMapper;

    @Transactional(readOnly = true)
    public List<AssetResponse> searchAssets(String ticker, String sector, AssetType assetType) {
        log.info("Searching assets with ticker: {}, sector: {}, type: {}", ticker, sector, assetType);
        return assetRepository.searchAssets(ticker, sector, assetType).stream()
                .map(assetMapper::toResponse)
                .collect(Collectors.toList());
    }
}
