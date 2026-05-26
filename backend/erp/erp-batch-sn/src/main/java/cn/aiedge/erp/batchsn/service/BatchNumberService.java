package cn.aiedge.erp.batchsn.service;

import cn.aiedge.erp.batchsn.entity.BatchNumber;
import com.baomidou.mybatisplus.extension.service.IService;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 批次号服务接口
 * 
 * @author team-member
 * @date 2026-04-27
 */
public interface BatchNumberService extends IService<BatchNumber> {
    
    /**
     * 创建批次
     */
    BatchNumber createBatch(BatchNumber batch);
    
    /**
     * 根据ID查询批次详情
     */
    BatchNumber getBatchById(Long id);
    
    /**
     * 查询批次列表（支持多条件筛选）
     */
    List<BatchNumber> listBatches(
        String batchNo,
        String productCode,
        String status,
        String sourceType,
        int page,
        int size
    );
    
    /**
     * 更新批次信息
     */
    BatchNumber updateBatch(Long id, BatchNumber batch);
    
    /**
     * 批量更新批次状态
     */
    int updateBatchStatus(List<Long> batchIds, String newStatus);
    
    /**
     * 批次入库操作
     */
    BatchNumber inbound(BatchNumber batch, Long warehouseId, String warehouseName, Long locationId);
    
    /**
     * 批次出库操作
     */
    BatchNumber outbound(Long batchId, BigDecimal quantity, Long warehouseId, Long locationId);
    
    /**
     * 质检操作
     */
    BatchNumber qualityInspection(Long batchId, String status, String inspectorId, String inspectorName);
    
    /**
     * 批次转移操作
     */
    BatchNumber transfer(Long batchId, Long fromWarehouseId, Long toWarehouseId, Long fromLocationId, Long toLocationId, BigDecimal quantity, String remark);
    
    /**
     * 批次盘点操作
     */
    BatchNumber inventory(Long batchId, BigDecimal physicalQuantity, BigDecimal adjustmentQuantity, String adjustmentReason, String operatorId, String operatorName);
    
    /**
     * 查询即将过期的批次（临期预警）
     */
    List<BatchNumber> getExpiringBatches(int warningDays);
    
    /**
     * 查询过期批次
     */
    List<BatchNumber> getExpiredBatches();
    
    /**
     * 查询批次库存汇总
     */
    List<BatchNumber> getStockSummary();
    
    /**
     * 查询批次流转记录
     */
    List<BatchNumber> getBatchFlowHistory(Long batchId);
    
    /**
     * 查询产品批次历史
     */
    List<BatchNumber> getProductBatchHistory(Long productId, String productCode);
    
    /**
     * 验证批次号是否存在
     */
    boolean batchNoExists(String batchNo);
    
    /**
     * 检查批次是否临期
     */
    boolean isBatchExpiring(BatchNumber batch, int warningDays);
    
    /**
     * 批次过期处理
     */
    int expireBatch(Long batchId);
    
    /**
     * 批量过期处理
     */
    int expireBatches(List<Long> batchIds, Date currentTime);
    
    /**
     * 高级搜索（支持复杂查询条件）
     */
    List<BatchNumber> searchBatches(
        String batchNo,
        String productCode,
        String productName,
        String status,
        String qualityStatus,
        String sourceType,
        Date productionDateStart,
        Date productionDateEnd,
        Date expirationDateStart,
        Date expirationDateEnd,
        Long warehouseId,
        String warehouseName,
        String sortField,
        String sortDirection,
        int page,
        int size
    );
    
    /**
     * 查询批次可用数量
     */
    BigDecimal getAvailableQuantity(Long batchId);
    
    /**
     * 查询批次预留数量
     */
    BigDecimal getReservedQuantity(Long batchId);
}
