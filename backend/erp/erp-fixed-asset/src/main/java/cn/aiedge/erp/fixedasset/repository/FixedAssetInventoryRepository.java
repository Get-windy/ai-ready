package cn.aiedge.erp.fixedasset.repository;

import cn.aiedge.erp.fixedasset.model.FixedAssetInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 固定资产盘点仓库
 */
@Repository
public interface FixedAssetInventoryRepository extends JpaRepository<FixedAssetInventory, Long>, JpaSpecificationExecutor<FixedAssetInventory> {

    List<FixedAssetInventory> findByAssetId(Long assetId);

    List<FixedAssetInventory> findByInventoryNo(String inventoryNo);

    List<FixedAssetInventory> findByStatus(String status);

    List<FixedAssetInventory> findByCheckResult(String checkResult);
}
