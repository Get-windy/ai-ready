package cn.aiedge.erp.batch.service.impl;

import cn.aiedge.erp.batch.model.BatchRecord;
import cn.aiedge.erp.batch.model.dto.CommonDTO;
import cn.aiedge.erp.batch.repository.BatchRecordRepository;
import cn.aiedge.erp.batch.service.BatchRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 批次记录服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchRecordServiceImpl implements BatchRecordService {
    
    private final BatchRecordRepository batchRecordRepository;
    
    private static final DateTimeFormatter BATCH_NUMBER_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMdd");
    
    @Override
    @Transactional
    public BatchRecord createBatchRecord(BatchRecord batchRecord) {
        // 验证批次号
        if (batchRecord.getBatchNumber() == null || batchRecord.getBatchNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("批次号不能为空");
        }
        
        // 检查批次号是否已存在
        if (isBatchNumberExists(batchRecord.getBatchNumber())) {
            throw new IllegalArgumentException("批次号已存在: " + batchRecord.getBatchNumber());
        }
        
        // 验证必要字段
        if (batchRecord.getProductId() == null) {
            throw new IllegalArgumentException("产品ID不能为空");
        }
        if (batchRecord.getProductionDate() == null) {
            throw new IllegalArgumentException("生产日期不能为空");
        }
        if (batchRecord.getExpiryDate() == null) {
            throw new IllegalArgumentException("有效期不能为空");
        }
        if (batchRecord.getQuantity() == null || batchRecord.getQuantity() <= 0) {
            throw new IllegalArgumentException("批次数量必须大于0");
        }
        
        // 设置默认值
        if (batchRecord.getStatus() == null) {
            batchRecord.setStatus(BatchRecord.BatchStatus.ACTIVE);
        }
        if (batchRecord.getQualityGrade() == null) {
            batchRecord.setQualityGrade(BatchRecord.QualityGrade.PENDING_INSPECTION);
        }
        
        // 设置剩余数量
        if (batchRecord.getRemainingQuantity() == null) {
            batchRecord.setRemainingQuantity(batchRecord.getQuantity());
        }
        
        // 保存批次记录
        BatchRecord savedBatch = batchRecordRepository.save(batchRecord);
        log.info("创建批次记录成功，批次号: {}, 批次ID: {}", 
                savedBatch.getBatchNumber(), savedBatch.getId());
        
        return savedBatch;
    }
    
    @Override
    @Transactional
    public BatchRecord updateBatchRecord(Long id, BatchRecord batchRecord) {
        BatchRecord existingBatch = getBatchRecord(id);
        
        // 验证批次状态是否允许修改
        if (existingBatch.getStatus() == BatchRecord.BatchStatus.CANCELLED ||
            existingBatch.getStatus() == BatchRecord.BatchStatus.CONSUMED) {
            throw new IllegalArgumentException("已取消或已消耗的批次不允许修改");
        }
        
        // 更新可修改字段
        existingBatch.setSupplierId(batchRecord.getSupplierId());
        existingBatch.setWarehouseId(batchRecord.getWarehouseId());
        existingBatch.setLocation(batchRecord.getLocation());
        existingBatch.setRemark(batchRecord.getRemark());
        existingBatch.setAttributes(batchRecord.getAttributes());
        
        // 更新时间
        BatchRecord updatedBatch = batchRecordRepository.save(existingBatch);
        log.info("更新批次记录成功，批次ID: {}", id);
        
        return updatedBatch;
    }
    
    @Override
    public BatchRecord getBatchRecord(Long id) {
        return batchRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("批次记录不存在，ID: " + id));
    }
    
    @Override
    public BatchRecord getBatchRecordByBatchNumber(String batchNumber) {
        return batchRecordRepository.findByBatchNumber(batchNumber)
                .orElseThrow(() -> new IllegalArgumentException("批次记录不存在，批次号: " + batchNumber));
    }
    
    @Override
    @Transactional
    public void deleteBatchRecord(Long id) {
        if (!batchRecordRepository.existsById(id)) {
            throw new IllegalArgumentException("批次记录不存在，ID: " + id);
        }
        
        batchRecordRepository.deleteById(id);
        log.info("删除批次记录成功，批次ID: {}", id);
    }
    
    @Override
    public List<BatchRecord> listBatchRecords() {
        return batchRecordRepository.findAll();
    }
    
    @Override
    public Page<BatchRecord> pageBatchRecords(CommonDTO.PageRequest pageRequest) {
        PageRequest pageable = PageRequest.of(
            pageRequest.getPage() - 1, // 转换为0-based
            pageRequest.getSize(),
            pageRequest.getSort()
        );
        return batchRecordRepository.findAll(pageable);
    }
    
    @Override
    public List<BatchRecord> listBatchRecordsByProduct(Long productId) {
        return batchRecordRepository.findByProductId(productId);
    }
    
    @Override
    public List<BatchRecord> listBatchRecordsBySupplier(Long supplierId) {
        return batchRecordRepository.findBySupplierId(supplierId);
    }
    
    @Override
    public List<BatchRecord> listBatchRecordsByStatus(BatchRecord.BatchStatus status) {
        return batchRecordRepository.findByStatus(status);
    }
    
    @Override
    public List<BatchRecord> listBatchRecordsByQualityGrade(BatchRecord.QualityGrade qualityGrade) {
        return batchRecordRepository.findByQualityGrade(qualityGrade);
    }
    
    @Override
    public List<BatchRecord> listNearExpiryBatches(int daysBeforeExpiry) {
        LocalDate expiryThreshold = LocalDate.now().plusDays(daysBeforeExpiry);
        return batchRecordRepository.findByExpiryDateBetweenAndStatus(
            LocalDate.now(), expiryThreshold, BatchRecord.BatchStatus.ACTIVE);
    }
    
    @Override
    public List<BatchRecord> listExpiredBatches() {
        LocalDate today = LocalDate.now();
        return batchRecordRepository.findByExpiryDateBeforeAndStatus(
            today, BatchRecord.BatchStatus.ACTIVE);
    }
    
    @Override
    @Transactional
    public BatchRecord updateBatchStatus(Long id, BatchRecord.BatchStatus status) {
        BatchRecord batchRecord = getBatchRecord(id);
        
        // 验证状态转换是否有效
        validateStatusTransition(batchRecord.getStatus(), status);
        
        batchRecord.setStatus(status);
        BatchRecord updatedBatch = batchRecordRepository.save(batchRecord);
        log.info("更新批次状态成功，批次ID: {}, 原状态: {}, 新状态: {}", 
                id, batchRecord.getStatus(), status);
        
        return updatedBatch;
    }
    
    @Override
    @Transactional
    public BatchRecord updateBatchQualityGrade(Long id, BatchRecord.QualityGrade qualityGrade) {
        BatchRecord batchRecord = getBatchRecord(id);
        
        batchRecord.setQualityGrade(qualityGrade);
        BatchRecord updatedBatch = batchRecordRepository.save(batchRecord);
        log.info("更新批次质量等级成功，批次ID: {}, 质量等级: {}", id, qualityGrade);
        
        return updatedBatch;
    }
    
    @Override
    @Transactional
    public BatchRecord adjustBatchQuantity(Long id, Integer quantityAdjustment, String remark) {
        BatchRecord batchRecord = getBatchRecord(id);
        
        // 验证批次状态
        if (batchRecord.getStatus() != BatchRecord.BatchStatus.ACTIVE) {
            throw new IllegalArgumentException("非活动批次不能进行库存调整");
        }
        
        // 计算新的剩余数量
        int newRemainingQuantity = batchRecord.getRemainingQuantity() + quantityAdjustment;
        
        // 验证库存数量
        if (newRemainingQuantity < 0) {
            throw new IllegalArgumentException("库存调整后数量不能小于0");
        }
        if (newRemainingQuantity > batchRecord.getQuantity()) {
            throw new IllegalArgumentException("库存调整后数量不能大于批次总量");
        }
        
        batchRecord.setRemainingQuantity(newRemainingQuantity);
        
        // 如果剩余数量为0，自动更新状态为已消耗
        if (newRemainingQuantity == 0) {
            batchRecord.setStatus(BatchRecord.BatchStatus.CONSUMED);
        }
        
        // 添加备注
        if (remark != null && !remark.trim().isEmpty()) {
            batchRecord.setRemark(batchRecord.getRemark() != null ? 
                batchRecord.getRemark() + "; " + remark : remark);
        }
        
        BatchRecord updatedBatch = batchRecordRepository.save(batchRecord);
        log.info("调整批次库存成功，批次ID: {}, 调整数量: {}, 剩余数量: {}", 
                id, quantityAdjustment, newRemainingQuantity);
        
        return updatedBatch;
    }
    
    @Override
    @Transactional
    public BatchRecord transferBatch(Long id, Long targetWarehouseId, String targetLocation, String remark) {
        BatchRecord batchRecord = getBatchRecord(id);
        
        // 验证批次状态
        if (batchRecord.getStatus() != BatchRecord.BatchStatus.ACTIVE) {
            throw new IllegalArgumentException("非活动批次不能进行转移");
        }
        
        batchRecord.setWarehouseId(targetWarehouseId);
        batchRecord.setLocation(targetLocation);
        
        // 添加备注
        if (remark != null && !remark.trim().isEmpty()) {
            batchRecord.setRemark(batchRecord.getRemark() != null ? 
                batchRecord.getRemark() + "; " + remark : remark);
        }
        
        BatchRecord updatedBatch = batchRecordRepository.save(batchRecord);
        log.info("转移批次成功，批次ID: {}, 目标仓库: {}, 目标库位: {}", 
                id, targetWarehouseId, targetLocation);
        
        return updatedBatch;
    }
    
    @Override
    public boolean isBatchNumberExists(String batchNumber) {
        return batchRecordRepository.existsByBatchNumber(batchNumber);
    }
    
    @Override
    public String generateBatchNumber(Long productId, LocalDate productionDate) {
        // 格式: BATCH-YYYYMMDD-PRODUCTID-RANDOM
        String datePart = productionDate.format(BATCH_NUMBER_FORMATTER);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("BATCH-%s-%d-%s", datePart, productId, randomPart);
    }
    
    @Override
    public CommonDTO.ApiResponse<Object> getBatchStatistics(Long productId) {
        // 这里实现批次统计逻辑
        // 实际实现需要查询数据库计算统计信息
        return CommonDTO.ApiResponse.success("批次统计功能待实现");
    }
    
    /**
     * 验证批次状态转换是否有效
     */
    private void validateStatusTransition(BatchRecord.BatchStatus fromStatus, 
                                         BatchRecord.BatchStatus toStatus) {
        // 定义有效的状态转换
        boolean isValid = true;
        
        switch (fromStatus) {
            case ACTIVE:
                // 有效批次可以转换为：已过期、隔离、取消、已消耗
                isValid = toStatus != BatchRecord.BatchStatus.ACTIVE;
                break;
            case EXPIRED:
                // 已过期批次只能保持过期状态
                isValid = false;
                break;
            case QUARANTINED:
                // 隔离批次可以转换为：合格批次或取消
                isValid = toStatus == BatchRecord.BatchStatus.ACTIVE || 
                         toStatus == BatchRecord.BatchStatus.CANCELLED;
                break;
            case CANCELLED:
                // 已取消批次不能再改变状态
                isValid = false;
                break;
            case CONSUMED:
                // 已消耗批次不能再改变状态
                isValid = false;
                break;
        }
        
        if (!isValid) {
            throw new IllegalArgumentException(
                String.format("无效的状态转换: %s -> %s", fromStatus, toStatus));
        }
    }
}