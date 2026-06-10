package cn.aiedge.erp.fixedasset.repository;

import cn.aiedge.erp.fixedasset.model.FixedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 固定资产仓库
 */
@Repository
public interface FixedAssetRepository extends JpaRepository<FixedAsset, Long>, JpaSpecificationExecutor<FixedAsset> {

    Optional<FixedAsset> findByAssetCode(String assetCode);

    List<FixedAsset> findByStatus(String status);

    List<FixedAsset> findByCategoryId(Long categoryId);

    List<FixedAsset> findByDepartmentId(String departmentId);

    List<FixedAsset> findByCustodianId(String custodianId);

    List<FixedAsset> findByUseStatus(String useStatus);

    @Query("SELECT COUNT(f) FROM FixedAsset f WHERE f.deleted = false")
    long countNotDeleted();

    @Query("SELECT COUNT(f) FROM FixedAsset f WHERE f.status = ?1 AND f.deleted = false")
    long countByStatusAndNotDeleted(String status);

    @Query("SELECT COALESCE(SUM(f.originalValue), 0) FROM FixedAsset f WHERE f.deleted = false")
    BigDecimal sumOriginalValue();

    @Query("SELECT COALESCE(SUM(f.netValue), 0) FROM FixedAsset f WHERE f.deleted = false")
    BigDecimal sumNetValue();

    @Query("SELECT COALESCE(SUM(f.accumulatedDepreciation), 0) FROM FixedAsset f WHERE f.deleted = false")
    BigDecimal sumAccumulatedDepreciation();
}
