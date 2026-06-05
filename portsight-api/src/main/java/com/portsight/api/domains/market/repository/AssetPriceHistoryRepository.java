package com.portsight.api.domains.market.repository;

import com.portsight.api.domains.market.entity.AssetPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetPriceHistoryRepository extends JpaRepository<AssetPriceHistory, UUID> {

    List<AssetPriceHistory> findByAssetIdOrderByRecordDateAsc(UUID assetId);

    Optional<AssetPriceHistory> findByAssetIdAndRecordDate(UUID assetId, LocalDate recordDate);

    List<AssetPriceHistory> findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
            UUID assetId, LocalDate from, LocalDate to);
}