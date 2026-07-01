package com.vanh.itam.asset.repository;

import com.vanh.itam.asset.entity.AssetImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssetImageRepository extends JpaRepository<AssetImage, Long> {

    @Query("SELECT i FROM AssetImage i WHERE i.asset.id = :assetId AND i.deletedAt IS NULL")
    List<AssetImage> findActiveByAssetId(@Param("assetId") Long assetId);

    @Query("SELECT COUNT(i) FROM AssetImage i WHERE i.asset.id = :assetId AND i.deletedAt IS NULL")
    long countActiveByAssetId(@Param("assetId") Long assetId);
}
