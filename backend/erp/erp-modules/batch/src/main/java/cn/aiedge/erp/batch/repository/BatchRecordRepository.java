package cn.aiedge.erp.batch.repository;

import cn.aiedge.erp.batch.model.BatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 批次记录仓库接口
 */
@Repository
public interface BatchRecordRepository extends JpaRepository<BatchRecord, Long>, JpaSpecificationExecutor<BatchRecord> {
    
    /**
     * 根据批次号查询批次记录
     */
    Optional<BatchRecord> findByBatchNumber(String batchNumber);
    
    /**
     * 根据批次号和租户ID查询批次记录
     */
    Optional<BatchRecord> findByBatchNumberAndTenantId(String batchNumber, String tenantId);
    
    /**
     * 查询特定产品的批次记录
     */
    List<BatchRecord> findByProductId(Long productId);
    
    /**
     * 查询特定供应商的批次记录
     */
    List<BatchRecord> findBySupplierId(Long supplierId);
    
    /**
     * 根据批次状态查询批次记录
     */
    List<BatchRecord> findByStatus(BatchRecord.BatchStatus status);
    
    /**
     * 查询即将过期的批次（指定天数内过期）
     */
    @Query("SELECT b FROM BatchRecord b WHERE b.expiryDate BETWEEN :today AND :expiryDate AND b.status = 'ACTIVE' AND b.qualityGrade = 'QUALIFIED'")
    List<BatchRecord> findNearExpiryBatches(@Param("today") LocalDate today, 
                                           @Param("expiryDate") LocalDate expiryDate);
    
    /**
     * 查询已过期的批次
     */
    @Query("SELECT b FROM BatchRecord b WHERE b.expiryDate < :today AND b.status = 'ACTIVE'")
    List<BatchRecord> findExpiredBatches(@Param("today") LocalDate today);
    
    /**
     * 统计批次库存数量
     */
    @Query("SELECT COUNT(b) FROM BatchRecord b WHERE b.productId = :productId AND b.status = 'ACTIVE'")
    long countActiveBatchesByProduct(@Param("productId") Long productId);
    
    /**
     * 统计批次总数量
     */
    @Query("SELECT COALESCE(SUM(b.remainingQuantity), 0) FROM BatchRecord b WHERE b.productId = :productId AND b.status = 'ACTIVE'")
    Integer sumRemainingQuantityByProduct(@Param("productId") Long productId);
    
    /**
     * 检查批次号是否存在
     */
    boolean existsByBatchNumber(String batchNumber);
    
    /**
     * 检查批次号是否存在（带租户）
     */
    boolean existsByBatchNumberAndTenantId(String batchNumber, String tenantId);
    
    /**
     * 根据产品ID和状态查询批次记录
     */
    List<BatchRecord> findByProductIdAndStatus(Long productId, BatchRecord.BatchStatus status);
    
    /**
     * 根据产品ID和质量等级查询批次记录
     */
    List<BatchRecord> findByProductIdAndQualityGrade(Long productId, BatchRecord.QualityGrade qualityGrade);
    
    /**
     * 根据仓库ID查询批次记录
     */
    List<BatchRecord> findByWarehouseId(Long warehouseId);
    
    /**
     * 根据生产日期范围查询批次记录
     */
    List<BatchRecord> findByProductionDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * 根据有效期范围查询批次记录
     */
    List<BatchRecord> findByExpiryDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * 根据产品ID列表查询批次记录
     */
    List<BatchRecord> findByProductIdIn(List<Long> productIds);
}