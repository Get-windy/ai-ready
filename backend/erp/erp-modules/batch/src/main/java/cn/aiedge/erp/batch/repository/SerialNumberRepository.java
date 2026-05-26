package cn.aiedge.erp.batch.repository;

import cn.aiedge.erp.batch.model.SerialNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 序列号仓库接口
 */
@Repository
public interface SerialNumberRepository extends JpaRepository<SerialNumber, Long>, JpaSpecificationExecutor<SerialNumber> {
    
    /**
     * 根据序列号查询
     */
    Optional<SerialNumber> findBySerialNumber(String serialNumber);
    
    /**
     * 根据序列号和租户ID查询
     */
    Optional<SerialNumber> findBySerialNumberAndTenantId(String serialNumber, String tenantId);
    
    /**
     * 根据批次ID查询序列号
     */
    List<SerialNumber> findByBatchId(Long batchId);
    
    /**
     * 根据产品ID查询序列号
     */
    List<SerialNumber> findByProductId(Long productId);
    
    /**
     * 根据序列号状态查询
     */
    List<SerialNumber> findByStatus(SerialNumber.SerialNumberStatus status);
    
    /**
     * 根据序列号类型查询
     */
    List<SerialNumber> findBySerialType(SerialNumber.SerialNumberType serialType);
    
    /**
     * 查询特定订单的序列号
     */
    List<SerialNumber> findByOrderId(Long orderId);
    
    /**
     * 查询特定客户的序列号
     */
    List<SerialNumber> findByCustomerId(Long customerId);
    
    /**
     * 检查序列号是否存在
     */
    boolean existsBySerialNumber(String serialNumber);
    
    /**
     * 检查序列号是否存在（带租户）
     */
    boolean existsBySerialNumberAndTenantId(String serialNumber, String tenantId);
    
    /**
     * 根据批次ID和状态查询序列号
     */
    List<SerialNumber> findByBatchIdAndStatus(Long batchId, SerialNumber.SerialNumberStatus status);
    
    /**
     * 根据产品ID和状态查询序列号
     */
    List<SerialNumber> findByProductIdAndStatus(Long productId, SerialNumber.SerialNumberStatus status);
    
    /**
     * 统计批次下的序列号数量
     */
    @Query("SELECT COUNT(s) FROM SerialNumber s WHERE s.batchId = :batchId")
    long countByBatchId(@Param("batchId") Long batchId);
    
    /**
     * 统计产品下的序列号数量
     */
    @Query("SELECT COUNT(s) FROM SerialNumber s WHERE s.productId = :productId")
    long countByProductId(@Param("productId") Long productId);
    
    /**
     * 统计特定状态的序列号数量
     */
    @Query("SELECT COUNT(s) FROM SerialNumber s WHERE s.status = :status")
    long countByStatus(@Param("status") SerialNumber.SerialNumberStatus status);
    
    /**
     * 根据序列号列表查询
     */
    List<SerialNumber> findBySerialNumberIn(List<String> serialNumbers);
    
    /**
     * 查询需要维护的序列号
     */
    @Query("SELECT s FROM SerialNumber s WHERE s.status = 'MAINTAINED' AND s.deactivationDate IS NULL")
    List<SerialNumber> findMaintainedSerialNumbers();
    
    /**
     * 查询需要激活的序列号
     */
    @Query("SELECT s FROM SerialNumber s WHERE s.status = 'AVAILABLE' AND s.activationDate IS NULL")
    List<SerialNumber> findAvailableSerialNumbers();
    
    /**
     * 根据位置查询序列号
     */
    List<SerialNumber> findByLocationContaining(String location);
    
    /**
     * 根据扫描次数查询序列号
     */
    List<SerialNumber> findByScanCountGreaterThan(Integer scanCount);
}