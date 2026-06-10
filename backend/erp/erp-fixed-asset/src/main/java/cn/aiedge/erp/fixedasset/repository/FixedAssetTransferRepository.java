package cn.aiedge.erp.fixedasset.repository;

import cn.aiedge.erp.fixedasset.model.FixedAssetTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 固定资产转移仓库
 */
@Repository
public interface FixedAssetTransferRepository extends JpaRepository<FixedAssetTransfer, Long>, JpaSpecificationExecutor<FixedAssetTransfer> {

    List<FixedAssetTransfer> findByAssetId(Long assetId);

    List<FixedAssetTransfer> findByStatus(String status);

    List<FixedAssetTransfer> findByTransferNo(String transferNo);
}
