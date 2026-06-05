package com.portsight.api.domains.asset.repository;

import com.portsight.api.domains.asset.entity.Asset;
import com.portsight.api.domains.asset.enums.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {

       Optional<Asset> findByTicker(String ticker);

       @Query("SELECT a FROM Asset a WHERE " +
                     "(:ticker IS NULL OR a.ticker = :ticker) AND " +
                     "(:sector IS NULL OR a.sector = :sector) AND " +
                     "(:assetType IS NULL OR a.assetType = :assetType)")
       List<Asset> searchAssets(@Param("ticker") String ticker,
                     @Param("sector") String sector,
                     @Param("assetType") AssetType assetType);
}