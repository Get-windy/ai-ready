package cn.aiedge.erp.fixedasset.repository;

import cn.aiedge.erp.fixedasset.model.FixedAssetPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * 固定资产购置申请仓库
 */
@Repository
public interface FixedAssetPurchaseRepository extends JpaRepository<FixedAssetPurchase, Long>, JpaSpecificationExecutor<FixedAssetPurchase> {

    Optional<FixedAssetPurchase> findByPurchaseNo(String purchaseNo);

    @Query("SELECT COUNT(p) FROM FixedAssetPurchase p WHERE p.status = ?1 AND p.deleted = false")
    long countByStatus(String status);

    @Query("SELECT COUNT(p) FROM FixedAssetPurchase p WHERE p.deleted = false")
    long countNotDeleted();
}
