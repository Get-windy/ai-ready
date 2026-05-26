package cn.aiedge.erp.batch.service;

import cn.aiedge.erp.batch.model.BatchRecord;
import cn.aiedge.erp.batch.model.dto.CommonDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

/**
 * 批次记录服务接口
 */
public interface BatchRecordService {
    
    /**
     * 创建批次记录
     */
    BatchRecord createBatchRecord(BatchRecord batchRecord);
    
    /**
     * 更新批次记录
     */
    BatchRecord updateBatchRecord(Long id, BatchRecord batchRecord);
    
    /**
     * 获取批次记录
     */
    BatchRecord getBatchRecord(Long id);
    
    /**
     * 根据批次号获取批次记录
     */
    BatchRecord getBatchRecordByBatchNumber(String batchNumber);
    
    /**
     * 删除批次记录（逻辑删除）
     */
    void deleteBatchRecord(Long id);
    
    /**
     * 查询批次记录列表
     */
    List<BatchRecord> listBatchRecords();
    
    /**
     * 分页查询批次记录
     */
    Page<BatchRecord> pageBatchRecords(CommonDTO.PageRequest pageRequest);
    
    /**
     * 查询产品批次记录
     */
    List<BatchRecord> listBatchRecordsByProduct(Long productId);
    
    /**
     * 查询供应商批次记录
     */
    List<BatchRecord> listBatchRecordsBySupplier(Long supplierId);
    
    /**
     * 查询批次状态记录
     */
    List<BatchRecord> listBatchRecordsByStatus(BatchRecord.BatchStatus status);
    
    /**
     * 查询质量等级记录
     */
    List<BatchRecord> listBatchRecordsByQualityGrade(BatchRecord.QualityGrade qualityGrade);
    
    /**
     * 查询即将过期批次
     */
    List<BatchRecord> listNearExpiryBatches(int daysBeforeExpiry);
    
    /**
     * 查询已过期批次
     */
    List<BatchRecord> listExpiredBatches();
    
    /**
     * 更新批次状态
     */
    BatchRecord updateBatchStatus(Long id, BatchRecord.BatchStatus status);
    
    /**
     * 更新批次质量等级
     */
    BatchRecord updateBatchQualityGrade(Long id, BatchRecord.QualityGrade qualityGrade);
    
    /**
     * 批次库存调整
     */
    BatchRecord adjustBatchQuantity(Long id, Integer quantityAdjustment, String remark);
    
    /**
     * 批次转移
     */
    BatchRecord transferBatch(Long id, Long targetWarehouseId, String targetLocation, String remark);
    
    /**
     * 检查批次号是否已存在
     */
    boolean isBatchNumberExists(String batchNumber);
    
    /**
     * 生成批次号
     */
    String generateBatchNumber(Long productId, LocalDate productionDate);
    
    /**
     * 统计批次信息
     */
    CommonDTO.ApiResponse<Object> getBatchStatistics(Long productId);
}