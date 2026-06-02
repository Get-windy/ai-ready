package cn.aiedge.erp.fixedasset.repository;

import cn.aiedge.erp.fixedasset.model.FixedAssetDepreciation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 固定资产折旧记录仓库
 */
@Repository
public interface FixedAssetDepreciationRepository extends JpaRepository<FixedAssetDepreciation, Long> {

    List<FixedAssetDepreciation> findByAssetIdOrderByPeriodDesc(Long assetId);

    List<FixedAssetDepreciation> findByPeriod(String period);

    Optional<FixedAssetDepreciation> findByAssetIdAndPeriod(Long assetId, String period);

    List<FixedAssetDepreciation> findByAssetIdAndPeriodOrderByCreatedAtAsc(Long assetId, String period);
}
