package cn.aiedge.inventory.repository;

import cn.aiedge.inventory.model.InventoryReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 库存报告数据访问接口
 * 提供库存报告相关的数据库操作
 */
@Repository
public interface InventoryReportRepository extends JpaRepository<InventoryReport, Long> {
    
    /**
     * 根据报告编号查找库存报告
     * @param reportNo 报告编号
     * @return 库存报告
     */
    Optional<InventoryReport> findByReportNo(String reportNo);
    
    /**
     * 根据仓库ID查找库存报告
     * @param warehouseId 仓库ID
     * @param pageable 分页参数
     * @return 分页的库存报告列表
     */
    Page<InventoryReport> findByWarehouseId(Long warehouseId, Pageable pageable);
    
    /**
     * 根据报告类型查找库存报告
     * @param reportType 报告类型：1-日报 2-周报 3-月报 4-年报 5-临时报告
     * @param pageable 分页参数
     * @return 分页的库存报告列表
     */
    Page<InventoryReport> findByReportType(Integer reportType, Pageable pageable);
    
    /**
     * 根据状态查找库存报告
     * @param status 状态：1-待审核 2-已审核 3-已发布 4-已归档 5-已作废
     * @param pageable 分页参数
     * @return 分页的库存报告列表
     */
    Page<InventoryReport> findByStatus(Integer status, Pageable pageable);
    
    /**
     * 根据报告时间范围查找库存报告
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 分页的库存报告列表
     */
    Page<InventoryReport> findByReportTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 根据仓库ID和报告类型查找最新的库存报告
     * @param warehouseId 仓库ID
     * @param reportType 报告类型
     * @return 最新的库存报告
     */
    @Query("SELECT ir FROM InventoryReport ir WHERE ir.warehouseId = :warehouseId AND ir.reportType = :reportType ORDER BY ir.reportTime DESC")
    Optional<InventoryReport> findLatestByWarehouseIdAndReportType(@Param("warehouseId") Long warehouseId, @Param("reportType") Integer reportType);
    
    /**
     * 查找指定时间段内未处理的库存报告
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param status 状态（待审核）
     * @return 未处理的库存报告列表
     */
    List<InventoryReport> findByReportTimeBetweenAndStatus(LocalDateTime startTime, LocalDateTime endTime, Integer status);
    
    /**
     * 根据仓库ID和状态统计报告数量
     * @param warehouseId 仓库ID
     * @param status 状态
     * @return 报告数量
     */
    long countByWarehouseIdAndStatus(Long warehouseId, Integer status);
    
    /**
     * 查找超时未审核的报告
     * @param deadline 截止时间
     * @param status 待审核状态
     * @return 超时的库存报告列表
     */
    @Query("SELECT ir FROM InventoryReport ir WHERE ir.createTime <= :deadline AND ir.status = :status")
    List<InventoryReport> findTimeoutReports(@Param("deadline") LocalDateTime deadline, @Param("status") Integer status);
    
    /**
     * 查找需要归档的历史报告
     * @param archiveDeadline 归档截止时间
     * @param status 已发布状态
     * @return 需要归档的报告列表
     */
    @Query("SELECT ir FROM InventoryReport ir WHERE ir.reportTime <= :archiveDeadline AND ir.status = :status")
    List<InventoryReport> findReportsToArchive(@Param("archiveDeadline") LocalDateTime archiveDeadline, @Param("status") Integer status);
    
    /**
     * 批量更新报告状态
     * @param ids 报告ID列表
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     * @return 更新的记录数
     */
    @Query("UPDATE InventoryReport ir SET ir.status = :newStatus, ir.updateTime = CURRENT_TIMESTAMP WHERE ir.id IN :ids AND ir.status = :oldStatus")
    int updateStatusBatch(@Param("ids") List<Long> ids, @Param("oldStatus") Integer oldStatus, @Param("newStatus") Integer newStatus);
    
    /**
     * 根据仓库ID和产品ID查找包含特定产品的库存报告
     * @param warehouseId 仓库ID
     * @param productId 产品ID
     * @return 包含特定产品的库存报告列表
     */
    @Query("SELECT ir FROM InventoryReport ir JOIN ir.inventoryReportItems iri WHERE ir.warehouseId = :warehouseId AND iri.productId = :productId ORDER BY ir.reportTime DESC")
    List<InventoryReport> findByWarehouseIdAndProductId(@Param("warehouseId") Long warehouseId, @Param("productId") Long productId);
    
    /**
     * 根据仓库ID、报告类型和状态统计
     * @param warehouseId 仓库ID
     * @param reportType 报告类型
     * @param status 状态
     * @return 统计数量
     */
    long countByWarehouseIdAndReportTypeAndStatus(Long warehouseId, Integer reportType, Integer status);
    
    /**
     * 查找仓库的最新有效报告（已审核或已发布）
     * @param warehouseId 仓库ID
     * @param validStatuses 有效状态列表
     * @return 最新有效报告
     */
    @Query("SELECT ir FROM InventoryReport ir WHERE ir.warehouseId = :warehouseId AND ir.status IN :validStatuses ORDER BY ir.reportTime DESC, ir.createTime DESC")
    Optional<InventoryReport> findLatestValidReport(@Param("warehouseId") Long warehouseId, @Param("validStatuses") List<Integer> validStatuses);
    
    /**
     * 查找需要重新计算的报告（数据源有更新）
     * @param warehouseId 仓库ID
     * @param lastDataUpdateTime 数据最后更新时间
     * @param recalculationStatus 需要重新计算的状态
     * @return 需要重新计算的报告列表
     */
    @Query("SELECT ir FROM InventoryReport ir WHERE ir.warehouseId = :warehouseId AND ir.lastDataTime < :lastDataUpdateTime AND ir.status = :recalculationStatus")
    List<InventoryReport> findReportsNeedRecalculation(@Param("warehouseId") Long warehouseId, @Param("lastDataUpdateTime") LocalDateTime lastDataUpdateTime, @Param("recalculationStatus") Integer recalculationStatus);
    
    /**
     * 查找同一报告周期内是否存在重复报告
     * @param warehouseId 仓库ID
     * @param reportType 报告类型
     * @param startTime 报告周期开始时间
     * @param endTime 报告周期结束时间
     * @param reportId 排除的报告ID（用于更新操作）
     * @return 是否存在重复
     */
    @Query("SELECT COUNT(ir) > 0 FROM InventoryReport ir WHERE ir.warehouseId = :warehouseId AND ir.reportType = :reportType AND ir.reportTime BETWEEN :startTime AND :endTime AND (:reportId IS NULL OR ir.id != :reportId)")
    boolean existsDuplicateReport(@Param("warehouseId") Long warehouseId, @Param("reportType") Integer reportType, 
                                 @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, 
                                 @Param("reportId") Long reportId);
}