package cn.aiedge.erp.batch.repository;

import cn.aiedge.erp.batch.model.BatchInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 批次库存仓库接口
 */
@Repository
public interface BatchInventoryRepository extends JpaRepository<BatchInventory, Long>, JpaSpecificationExecutor<BatchInventory> {
    
    /**
     * 根据批次ID查询库存
     */
    List<BatchInventory> findByBatchId(Long batchId);
    
    /**
     * 根据产品ID查询库存
     */
    List<BatchInventory> findByProductId(Long productId);
    
    /**
     * 根据仓库ID查询库存
     */
    List<BatchInventory> findByWarehouseId(Long warehouseId);
    
    /**
     * 根据库存状态查询
     */
    List<BatchInventory> findByInventoryStatus(BatchInventory.InventoryStatus inventoryStatus);
    
    /**
     * 根据批次ID和仓库ID查询库存
     */
    Optional<BatchInventory> findByBatchIdAndWarehouseId(Long batchId, Long warehouseId);
    
    /**
     * 根据产品ID和仓库ID查询库存
     */
    List<BatchInventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId);
    
    /**
     * 根据产品ID、仓库ID和库存状态查询
     */
    List<BatchInventory> findByProductIdAndWarehouseIdAndInventoryStatus(
            Long productId, Long warehouseId, BatchInventory.InventoryStatus inventoryStatus);
    
    /**
     * 查询临期库存
     */
    @Query("SELECT bi FROM BatchInventory bi WHERE bi.isNearExpiry = true AND bi.isExpired = false")
    List<BatchInventory> findNearExpiryInventories();
    
    /**
     * 查询已过期库存
     */
    @Query("SELECT bi FROM BatchInventory bi WHERE bi.isExpired = true")
    List<BatchInventory> findExpiredInventories();
    
    /**
     * 查询可用库存（非锁定、非隔离、非预留）
     */
    @Query("SELECT bi FROM BatchInventory bi WHERE bi.inventoryStatus = 'AVAILABLE' AND bi.availableQuantity > 0")
    List<BatchInventory> findAvailableInventories();
    
    /**
     * 统计产品总库存数量
     */
    @Query("SELECT COALESCE(SUM(bi.availableQuantity), 0) FROM BatchInventory bi WHERE bi.productId = :productId AND bi.inventoryStatus = 'AVAILABLE'")
    Integer sumAvailableQuantityByProduct(@Param("productId") Long productId);
    
    /**
     * 统计产品总库存成本
     */
    @Query("SELECT COALESCE(SUM(bi.totalCost), 0) FROM BatchInventory bi WHERE bi.productId = :productId")
    Double sumTotalCostByProduct(@Param("productId") Long productId);
    
    /**
     * 统计仓库总库存数量
     */
    @Query("SELECT COALESCE(SUM(bi.availableQuantity), 0) FROM BatchInventory bi WHERE bi.warehouseId = :warehouseId AND bi.inventoryStatus = 'AVAILABLE'")
    Integer sumAvailableQuantityByWarehouse(@Param("warehouseId") Long warehouseId);
    
    /**
     * 统计特定批次的库存数量
     */
    @Query("SELECT COALESCE(SUM(bi.availableQuantity), 0) FROM BatchInventory bi WHERE bi.batchId = :batchId AND bi.inventoryStatus = 'AVAILABLE'")
    Integer sumAvailableQuantityByBatch(@Param("batchId") Long batchId);
    
    /**
     * 根据位置查询库存
     */
    List<BatchInventory> findByLocationContaining(String location);
    
    /**
     * 查询需要补货的库存（低于预警阈值）
     */
    @Query("SELECT bi FROM BatchInventory bi WHERE bi.warningThreshold IS NOT NULL AND bi.availableQuantity <= bi.warningThreshold")
    List<BatchInventory> findLowStockInventories();
    
    /**
     * 根据有效期范围查询库存
     */
    List<BatchInventory> findByExpiryDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 根据入库时间范围查询库存
     */
    List<BatchInventory> findByStockInDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 根据产品ID列表查询库存
     */
    List<BatchInventory> findByProductIdIn(List<Long> productIds);
    
    /**
     * 根据仓库ID列表查询库存
     */
    List<BatchInventory> findByWarehouseIdIn(List<Long> warehouseIds);
    
    /**
     * 根据批次ID列表查询库存
     */
    List<BatchInventory> findByBatchIdIn(List<Long> batchIds);
    
    /**
     * 根据租户ID和产品ID查询库存
     */
    List<BatchInventory> findByTenantIdAndProductId(String tenantId, Long productId);
}