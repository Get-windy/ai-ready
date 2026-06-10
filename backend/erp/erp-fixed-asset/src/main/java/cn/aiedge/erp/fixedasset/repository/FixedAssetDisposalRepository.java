package cn.aiedge.erp.fixedasset.repository;

import cn.aiedge.erp.fixedasset.model.FixedAssetDisposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 固定资产处置仓库
 */
@Repository
public interface FixedAssetDisposalRepository extends JpaRepository<FixedAssetDisposal, Long>, JpaSpecificationExecutor<FixedAssetDisposal> {

    List<FixedAssetDisposal> findByAssetId(Long assetId);

    List<FixedAssetDisposal> findByStatus(String status);

    List<FixedAssetDisposal> findByDisposalNo(String disposalNo);
}
