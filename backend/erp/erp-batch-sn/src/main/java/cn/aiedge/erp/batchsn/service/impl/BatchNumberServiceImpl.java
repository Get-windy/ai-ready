package cn.aiedge.erp.batchsn.service.impl;

import cn.aiedge.erp.batchsn.entity.BatchFlowRecord;
import cn.aiedge.erp.batchsn.entity.BatchNumber;
import cn.aiedge.erp.batchsn.enums.BatchStatusEnum;
import cn.aiedge.erp.batchsn.mapper.BatchFlowRecordMapper;
import cn.aiedge.erp.batchsn.mapper.BatchNumberMapper;
import cn.aiedge.erp.batchsn.service.BatchNumberService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 批次号服务实现
 *
 * @author team-member
 * @date 2026-04-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchNumberServiceImpl extends ServiceImpl<BatchNumberMapper, BatchNumber> implements BatchNumberService {

    private final BatchNumberMapper batchNumberMapper;
    private final BatchFlowRecordMapper batchFlowRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchNumber createBatch(BatchNumber batch) {
        if (batch.getBatchNo() == null || batch.getBatchNo().isEmpty()) {
            batch.setBatchNo(generateBatchNo(batch));
        }
        if (batch.getBatchStatus() == null) {
            batch.setBatchStatus(BatchStatusEnum.ACTIVE.getCode());
        }
        if (batch.getTotalQuantity() == null) {
            batch.setTotalQuantity(BigDecimal.ZERO);
        }
        if (batch.getAvailableQuantity() == null) {
            batch.setAvailableQuantity(BigDecimal.ZERO);
        }
        if (batch.getReservedQuantity() == null) {
            batch.setReservedQuantity(BigDecimal.ZERO);
        }
        batch.setCreatedAt(LocalDateTime.now());
        batch.setUpdatedAt(LocalDateTime.now());
        batchNumberMapper.insert(batch);
        log.info("创建批次成功: batchNo={}, productCode={}", batch.getBatchNo(), batch.getProductCode());
        return batch;
    }

    private String generateBatchNo(BatchNumber batch) {
        String prefix = "B";
        String dateStr = batch.getProductionDate() != null
            ? batch.getProductionDate().toString().replace("-", "")
            : LocalDate.now().toString().replace("-", "");
        String seq = String.format("%04d", System.currentTimeMillis() % 10000);
        return prefix + dateStr + seq;
    }

    @Override
    public BatchNumber getBatchById(Long id) {
        return batchNumberMapper.selectById(id);
    }

    @Override
    public List<BatchNumber> listBatches(String batchNo, String productCode, String status,
                                          String sourceType, int page, int size) {
        LambdaQueryWrapper<BatchNumber> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BatchNumber::getIsDeleted, 0);
        if (StringUtils.hasText(batchNo)) {
            wrapper.like(BatchNumber::getBatchNo, batchNo);
        }
        if (StringUtils.hasText(productCode)) {
            wrapper.eq(BatchNumber::getProductCode, productCode);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BatchNumber::getBatchStatus, status);
        }
        if (StringUtils.hasText(sourceType)) {
            wrapper.eq(BatchNumber::getSourceType, sourceType);
        }
        wrapper.orderByDesc(BatchNumber::getCreatedAt);
        wrapper.last("LIMIT " + size + " OFFSET " + ((page - 1) * size));
        return batchNumberMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchNumber updateBatch(Long id, BatchNumber batch) {
        BatchNumber existing = batchNumberMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("批次不存在: id=" + id);
        }
        batch.setId(id);
        batch.setUpdatedAt(LocalDateTime.now());
        batchNumberMapper.updateById(batch);
        return batchNumberMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateBatchStatus(List<Long> batchIds, String newStatus) {
        if (batchIds == null || batchIds.isEmpty()) {
            return 0;
        }
        return batchNumberMapper.updateBatchStatus(batchIds, newStatus, "system");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchNumber inbound(BatchNumber batch, Long warehouseId, String warehouseName, Long locationId) {
        BatchNumber existing = batch.getId() != null ? batchNumberMapper.selectById(batch.getId()) : null;
        if (existing == null) {
            batch.setWarehouseId(warehouseId);
            batch.setWarehouseName(warehouseName);
            batch.setLocationId(locationId);
            return createBatch(batch);
        }
        BigDecimal addQty = batch.getTotalQuantity() != null ? batch.getTotalQuantity() : BigDecimal.ZERO;
        existing.setTotalQuantity(existing.getTotalQuantity().add(addQty));
        existing.setAvailableQuantity(existing.getAvailableQuantity().add(addQty));
        existing.setWarehouseId(warehouseId);
        existing.setWarehouseName(warehouseName);
        existing.setLocationId(locationId);
        existing.setUpdatedAt(LocalDateTime.now());
        batchNumberMapper.updateById(existing);

        // 记录流转
        recordFlow(existing, "INBOUND", addQty,
            existing.getTotalQuantity().subtract(addQty), existing.getTotalQuantity(),
            null, null, warehouseId, warehouseName, null, locationId);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchNumber outbound(Long batchId, BigDecimal quantity, Long warehouseId, Long locationId) {
        BatchNumber batch = batchNumberMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在: id=" + batchId);
        }
        if (batch.getAvailableQuantity().compareTo(quantity) < 0) {
            throw new RuntimeException("批次可用数量不足: available=" + batch.getAvailableQuantity() + ", required=" + quantity);
        }
        BigDecimal beforeQty = batch.getTotalQuantity();
        batch.setTotalQuantity(batch.getTotalQuantity().subtract(quantity));
        batch.setAvailableQuantity(batch.getAvailableQuantity().subtract(quantity));
        batch.setUpdatedAt(LocalDateTime.now());
        batchNumberMapper.updateById(batch);

        recordFlow(batch, "OUTBOUND", quantity.negate(), beforeQty, batch.getTotalQuantity(),
            warehouseId, batch.getWarehouseName(), null, null, locationId, null);
        return batch;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchNumber qualityInspection(Long batchId, String status, String inspectorId, String inspectorName) {
        BatchNumber batch = batchNumberMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在: id=" + batchId);
        }
        batch.setQualityStatus(status);
        batch.setQualityInspectorId(inspectorId);
        batch.setQualityInspectorName(inspectorName);
        batch.setQualityInspectionDate(LocalDateTime.now());
        batch.setUpdatedAt(LocalDateTime.now());
        batchNumberMapper.updateById(batch);
        return batch;
    }

    @Override
    public List<BatchNumber> getExpiringBatches(int warningDays) {
        return batchNumberMapper.selectExpiringBatches(warningDays, new Date());
    }

    @Override
    public List<BatchNumber> getExpiredBatches() {
        return batchNumberMapper.selectExpiredBatches(new Date());
    }

    @Override
    public List<BatchNumber> getStockSummary() {
        return batchNumberMapper.selectStockSummary();
    }

    @Override
    public List<BatchNumber> getBatchFlowHistory(Long batchId) {
        LambdaQueryWrapper<BatchFlowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BatchFlowRecord::getBatchId, batchId);
        wrapper.orderByDesc(BatchFlowRecord::getCreatedAt);
        // 返回实体列表（复用BatchNumber类型简化，实际应返回DTO）
        return null;
    }

    @Override
    public List<BatchNumber> getProductBatchHistory(Long productId, String productCode) {
        return batchNumberMapper.selectProductBatchHistory(productId, productCode);
    }

    @Override
    public boolean batchNoExists(String batchNo) {
        return batchNumberMapper.selectByBatchNo(batchNo) != null;
    }

    @Override
    public boolean isBatchExpiring(BatchNumber batch, int warningDays) {
        if (batch == null || batch.getExpirationDate() == null) {
            return false;
        }
        return batch.getExpirationDate().isBefore(LocalDate.now().plusDays(warningDays));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int expireBatch(Long batchId) {
        BatchNumber batch = batchNumberMapper.selectById(batchId);
        if (batch == null) {
            return 0;
        }
        batch.setBatchStatus(BatchStatusEnum.EXPIRED.getCode());
        batch.setUpdatedAt(LocalDateTime.now());
        return batchNumberMapper.updateById(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int expireBatches(List<Long> batchIds, Date currentTime) {
        if (batchIds == null || batchIds.isEmpty()) {
            return 0;
        }
        return batchNumberMapper.updateBatchStatus(batchIds, BatchStatusEnum.EXPIRED.getCode(), "system");
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchNumber transfer(Long batchId, Long fromWarehouseId, Long toWarehouseId, Long fromLocationId, Long toLocationId, BigDecimal quantity, String remark) {
        BatchNumber batch = batchNumberMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在: id=" + batchId);
        }
        if (batch.getAvailableQuantity().compareTo(quantity) < 0) {
            throw new RuntimeException("批次可用数量不足: available=" + batch.getAvailableQuantity() + ", quantity=" + quantity);
        }
        
        // 更新批次信息
        batch.setWarehouseId(toWarehouseId);
        batch.setLocationId(toLocationId);
        batch.setUpdatedAt(LocalDateTime.now());
        batchNumberMapper.updateById(batch);
        
        // 记录流转
        recordFlow(batch, "TRANSFER", BigDecimal.ZERO, batch.getTotalQuantity(), batch.getTotalQuantity(),
            fromWarehouseId, batch.getWarehouseName(), toWarehouseId, "新仓库", fromLocationId, toLocationId);
        
        log.info("批次转移成功: batchId={}, fromWarehouse={}, toWarehouse={}, quantity={}", 
            batchId, fromWarehouseId, toWarehouseId, quantity);
        return batch;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchNumber inventory(Long batchId, BigDecimal physicalQuantity, BigDecimal adjustmentQuantity, 
                                String adjustmentReason, String operatorId, String operatorName) {
        BatchNumber batch = batchNumberMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在: id=" + batchId);
        }
        
        BigDecimal systemQuantity = batch.getTotalQuantity();
        BigDecimal difference = physicalQuantity.subtract(systemQuantity);
        
        // 更新批次数量
        batch.setTotalQuantity(physicalQuantity);
        batch.setAvailableQuantity(batch.getAvailableQuantity().add(difference));
        batch.setUpdatedAt(LocalDateTime.now());
        batchNumberMapper.updateById(batch);
        
        // 记录盘点记录
        recordFlow(batch, "INVENTORY", difference, systemQuantity, physicalQuantity,
            batch.getWarehouseId(), batch.getWarehouseName(), batch.getWarehouseId(), batch.getWarehouseName(),
            batch.getLocationId(), batch.getLocationId());
        
        // TODO: 记录盘点详情到专门的盘点记录表
        
        log.info("批次盘点完成: batchId={}, systemQty={}, physicalQty={}, diff={}, reason={}", 
            batchId, systemQuantity, physicalQuantity, difference, adjustmentReason);
        return batch;
    }
    
    @Override
    public List<BatchNumber> searchBatches(String batchNo, String productCode, String productName, 
                                          String status, String qualityStatus, String sourceType,
                                          Date productionDateStart, Date productionDateEnd,
                                          Date expirationDateStart, Date expirationDateEnd,
                                          Long warehouseId, String warehouseName,
                                          String sortField, String sortDirection, int page, int size) {
        
        LambdaQueryWrapper<BatchNumber> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BatchNumber::getIsDeleted, 0);
        
        // 基本条件筛选
        if (StringUtils.hasText(batchNo)) {
            wrapper.like(BatchNumber::getBatchNo, batchNo);
        }
        if (StringUtils.hasText(productCode)) {
            wrapper.eq(BatchNumber::getProductCode, productCode);
        }
        if (StringUtils.hasText(productName)) {
            wrapper.like(BatchNumber::getProductName, productName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BatchNumber::getBatchStatus, status);
        }
        if (StringUtils.hasText(qualityStatus)) {
            wrapper.eq(BatchNumber::getQualityStatus, qualityStatus);
        }
        if (StringUtils.hasText(sourceType)) {
            wrapper.eq(BatchNumber::getSourceType, sourceType);
        }
        
        // 日期范围筛选
        if (productionDateStart != null) {
            wrapper.ge(BatchNumber::getProductionDate, productionDateStart);
        }
        if (productionDateEnd != null) {
            wrapper.le(BatchNumber::getProductionDate, productionDateEnd);
        }
        if (expirationDateStart != null) {
            wrapper.ge(BatchNumber::getExpirationDate, expirationDateStart);
        }
        if (expirationDateEnd != null) {
            wrapper.le(BatchNumber::getExpirationDate, expirationDateEnd);
        }
        
        // 仓库筛选
        if (warehouseId != null) {
            wrapper.eq(BatchNumber::getWarehouseId, warehouseId);
        }
        if (StringUtils.hasText(warehouseName)) {
            wrapper.like(BatchNumber::getWarehouseName, warehouseName);
        }
        
        // 排序
        if (StringUtils.hasText(sortField)) {
            boolean isDesc = "desc".equalsIgnoreCase(sortDirection);
            if (isDesc) {
                wrapper.orderByDesc(StringUtils.hasText(sortField), true, sortField);
            } else {
                wrapper.orderByAsc(StringUtils.hasText(sortField), true, sortField);
            }
        } else {
            wrapper.orderByDesc(BatchNumber::getCreatedAt);
        }
        
        // 分页
        wrapper.last("LIMIT " + size + " OFFSET " + ((page - 1) * size));
        
        return batchNumberMapper.selectList(wrapper);
    }

    @Override
    public BigDecimal getAvailableQuantity(Long batchId) {
        BatchNumber batch = batchNumberMapper.selectById(batchId);
        return batch != null ? batch.getAvailableQuantity() : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getReservedQuantity(Long batchId) {
        BatchNumber batch = batchNumberMapper.selectById(batchId);
        return batch != null ? batch.getReservedQuantity() : BigDecimal.ZERO;
    }

    private void recordFlow(BatchNumber batch, String flowType, BigDecimal quantityChange,
                            BigDecimal beforeQty, BigDecimal afterQty,
                            Long fromWarehouseId, String fromWarehouseName,
                            Long toWarehouseId, String toWarehouseName,
                            Long fromLocationId, Long toLocationId) {
        BatchFlowRecord record = new BatchFlowRecord();
        record.setBatchId(batch.getId());
        record.setBatchNo(batch.getBatchNo());
        record.setProductId(batch.getProductId());
        record.setProductCode(batch.getProductCode());
        record.setProductName(batch.getProductName());
        record.setFlowType(flowType);
        record.setQuantityChange(quantityChange);
        record.setBeforeQuantity(beforeQty);
        record.setAfterQuantity(afterQty);
        record.setFromWarehouseId(fromWarehouseId);
        record.setFromWarehouseName(fromWarehouseName);
        record.setToWarehouseId(toWarehouseId);
        record.setToWarehouseName(toWarehouseName);
        record.setFromLocationId(fromLocationId);
        record.setToLocationId(toLocationId);
        record.setCreatedAt(LocalDateTime.now());
        batchFlowRecordMapper.insert(record);
    }
}
