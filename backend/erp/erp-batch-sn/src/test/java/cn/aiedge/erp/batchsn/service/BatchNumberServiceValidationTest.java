package cn.aiedge.erp.batchsn.service;

import cn.aiedge.erp.batchsn.entity.BatchNumber;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 批次号服务验证测试
 * 测试领域模型和业务规则的验证逻辑
 */
class BatchNumberServiceValidationTest {
    
    @Test
    void testBatchNumberEntityValidation() {
        // 测试实体类的基本验证逻辑
        
        BatchNumber batch = new BatchNumber();
        
        // 设置必要字段
        batch.setBatchNo("TEST-BATCH-001");
        batch.setProductCode("PROD-001");
        batch.setProductName("测试产品");
        batch.setBatchStatus("ACTIVE");
        batch.setTotalQuantity(new BigDecimal("1000.00"));
        batch.setAvailableQuantity(new BigDecimal("800.00"));
        batch.setReservedQuantity(new BigDecimal("200.00"));
        batch.setProductionDate(LocalDate.now().minusDays(30));
        batch.setExpirationDate(LocalDate.now().plusDays(365));
        batch.setSourceType("PRODUCTION");
        batch.setWarehouseName("中央仓库");
        batch.setCreatedBy("TEST_USER");
        batch.setCreatedAt(LocalDateTime.now());
        
        // 验证字段值
        assertEquals("TEST-BATCH-001", batch.getBatchNo());
        assertEquals("PROD-001", batch.getProductCode());
        assertEquals("ACTIVE", batch.getBatchStatus());
        assertEquals(new BigDecimal("1000.00"), batch.getTotalQuantity());
        assertEquals(new BigDecimal("800.00"), batch.getAvailableQuantity());
        assertEquals(new BigDecimal("200.00"), batch.getReservedQuantity());
        
        // 验证数量关系：可用数量 + 预留数量 <= 总数量
        BigDecimal totalCalculated = batch.getAvailableQuantity().add(batch.getReservedQuantity());
        assertTrue(batch.getTotalQuantity().compareTo(totalCalculated) >= 0,
            "总数量应大于等于可用数量加预留数量");
        
        // 验证日期关系：生产日期应早于有效期
        assertTrue(batch.getProductionDate().isBefore(batch.getExpirationDate()),
            "生产日期应早于有效期");
        
        // 验证有效期未过期
        assertTrue(batch.getExpirationDate().isAfter(LocalDate.now()),
            "有效期应晚于当前日期");
    }
    
    @Test
    void testBatchNumberBusinessRules() {
        // 测试业务规则
        
        // 规则1：批次状态必须为预定义值之一
        BatchNumber batch = new BatchNumber();
        batch.setBatchStatus("ACTIVE");
        
        assertTrue(isValidBatchStatus(batch.getBatchStatus()),
            "批次状态应为预定义的有效状态");
        
        // 测试有效状态
        assertTrue(isValidBatchStatus("ACTIVE"));
        assertTrue(isValidBatchStatus("EXPIRED"));
        assertTrue(isValidBatchStatus("QUARANTINED"));
        assertTrue(isValidBatchStatus("CANCELLED"));
        
        // 测试无效状态
        assertFalse(isValidBatchStatus("INVALID"));
        assertFalse(isValidBatchStatus(null));
        assertFalse(isValidBatchStatus(""));
        
        // 规则2：来源类型必须为预定义值之一
        batch.setSourceType("PRODUCTION");
        
        assertTrue(isValidSourceType(batch.getSourceType()),
            "来源类型应为预定义的有效类型");
        
        // 测试有效来源类型
        assertTrue(isValidSourceType("PURCHASE"));
        assertTrue(isValidSourceType("PRODUCTION"));
        assertTrue(isValidSourceType("SALE_RETURN"));
        
        // 规则3：质量状态必须为预定义值之一
        batch.setQualityStatus("NORMAL");
        
        assertTrue(isValidQualityStatus(batch.getQualityStatus()),
            "质量状态应为预定义的有效状态");
        
        // 测试有效质量状态
        assertTrue(isValidQualityStatus("NORMAL"));
        assertTrue(isValidQualityStatus("QUARANTINED"));
        assertTrue(isValidQualityStatus("DEFECTIVE"));
    }
    
    @Test
    void testBatchNumberQuantityValidation() {
        // 测试数量验证逻辑
        
        // 场景1：正常数量
        BatchNumber normalBatch = createBatchWithQuantities("1000.00", "800.00", "200.00");
        assertTrue(validateBatchQuantities(normalBatch),
            "正常数量关系应通过验证");
        
        // 场景2：可用数量超过总数量（无效）
        BatchNumber invalidBatch1 = createBatchWithQuantities("1000.00", "1200.00", "0.00");
        assertFalse(validateBatchQuantities(invalidBatch1),
            "可用数量超过总数量应验证失败");
        
        // 场景3：预留数量超过总数量（无效）
        BatchNumber invalidBatch2 = createBatchWithQuantities("1000.00", "0.00", "1200.00");
        assertFalse(validateBatchQuantities(invalidBatch2),
            "预留数量超过总数量应验证失败");
        
        // 场景4：可用+预留超过总数量（无效）
        BatchNumber invalidBatch3 = createBatchWithQuantities("1000.00", "600.00", "500.00");
        assertFalse(validateBatchQuantities(invalidBatch3),
            "可用数量+预留数量超过总数量应验证失败");
        
        // 场景5：数量为负数（无效）
        BatchNumber invalidBatch4 = createBatchWithQuantities("-100.00", "0.00", "0.00");
        assertFalse(validateBatchQuantities(invalidBatch4),
            "负数量应验证失败");
        
        // 场景6：可用数量为负数（无效）
        BatchNumber invalidBatch5 = createBatchWithQuantities("1000.00", "-100.00", "0.00");
        assertFalse(validateBatchQuantities(invalidBatch5),
            "负可用数量应验证失败");
        
        // 场景7：预留数量为负数（无效）
        BatchNumber invalidBatch6 = createBatchWithQuantities("1000.00", "0.00", "-100.00");
        assertFalse(validateBatchQuantities(invalidBatch6),
            "负预留数量应验证失败");
    }
    
    @Test
    void testBatchNumberDateValidation() {
        // 测试日期验证逻辑
        
        // 场景1：正常日期关系
        BatchNumber normalBatch = createBatchWithDates(
            LocalDate.now().minusDays(30),
            LocalDate.now().plusDays(365)
        );
        assertTrue(validateBatchDates(normalBatch),
            "正常日期关系应通过验证");
        
        // 场景2：生产日期晚于当前日期（无效）
        BatchNumber invalidBatch1 = createBatchWithDates(
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(366)
        );
        assertFalse(validateBatchDates(invalidBatch1),
            "未来生产日期应验证失败");
        
        // 场景3：有效期早于生产日期（无效）
        BatchNumber invalidBatch2 = createBatchWithDates(
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(60)
        );
        assertFalse(validateBatchDates(invalidBatch2),
            "有效期早于生产日期应验证失败");
        
        // 场景4：有效期早于当前日期（已过期）
        BatchNumber expiredBatch = createBatchWithDates(
            LocalDate.now().minusDays(60),
            LocalDate.now().minusDays(1)
        );
        assertFalse(validateBatchDates(expiredBatch),
            "已过期批次应验证失败");
        
        // 场景5：生产日期为null（有效）
        BatchNumber nullProductionBatch = new BatchNumber();
        nullProductionBatch.setExpirationDate(LocalDate.now().plusDays(365));
        assertTrue(validateBatchDates(nullProductionBatch),
            "生产日期为null应通过验证");
        
        // 场景6：有效期为null（有效）
        BatchNumber nullExpirationBatch = new BatchNumber();
        nullExpirationBatch.setProductionDate(LocalDate.now().minusDays(30));
        assertTrue(validateBatchDates(nullExpirationBatch),
            "有效期为null应通过验证");
    }
    
    @Test
    void testBatchNumberBusinessLogic() {
        // 测试业务逻辑
        
        // 逻辑1：计算剩余天数
        BatchNumber batch = new BatchNumber();
        batch.setExpirationDate(LocalDate.now().plusDays(30));
        
        long remainingDays = calculateRemainingDays(batch);
        assertEquals(30, remainingDays, "剩余天数计算错误");
        
        // 逻辑2：判断是否临期（7天预警）
        BatchNumber expiringBatch = new BatchNumber();
        expiringBatch.setExpirationDate(LocalDate.now().plusDays(5));
        
        assertTrue(isBatchExpiring(expiringBatch, 7),
            "5天后过期的批次应在7天预警期内");
        
        BatchNumber notExpiringBatch = new BatchNumber();
        notExpiringBatch.setExpirationDate(LocalDate.now().plusDays(10));
        
        assertFalse(isBatchExpiring(notExpiringBatch, 7),
            "10天后过期的批次不应在7天预警期内");
        
        // 逻辑3：判断批次是否可用（状态为ACTIVE且未过期）
        BatchNumber availableBatch = new BatchNumber();
        availableBatch.setBatchStatus("ACTIVE");
        availableBatch.setExpirationDate(LocalDate.now().plusDays(30));
        availableBatch.setAvailableQuantity(new BigDecimal("100.00"));
        
        assertTrue(isBatchAvailable(availableBatch),
            "活跃且未过期的批次应可用");
        
        BatchNumber expiredBatch = new BatchNumber();
        expiredBatch.setBatchStatus("ACTIVE");
        expiredBatch.setExpirationDate(LocalDate.now().minusDays(1));
        
        assertFalse(isBatchAvailable(expiredBatch),
            "已过期的批次不可用");
        
        BatchNumber inactiveBatch = new BatchNumber();
        inactiveBatch.setBatchStatus("CANCELLED");
        inactiveBatch.setExpirationDate(LocalDate.now().plusDays(30));
        
        assertFalse(isBatchAvailable(inactiveBatch),
            "非活跃状态的批次不可用");
        
        BatchNumber noInventoryBatch = new BatchNumber();
        noInventoryBatch.setBatchStatus("ACTIVE");
        noInventoryBatch.setExpirationDate(LocalDate.now().plusDays(30));
        noInventoryBatch.setAvailableQuantity(BigDecimal.ZERO);
        
        assertFalse(isBatchAvailable(noInventoryBatch),
            "无可用库存的批次不可用");
    }
    
    // 辅助方法
    
    private BatchNumber createBatchWithQuantities(String total, String available, String reserved) {
        BatchNumber batch = new BatchNumber();
        batch.setTotalQuantity(new BigDecimal(total));
        batch.setAvailableQuantity(new BigDecimal(available));
        batch.setReservedQuantity(new BigDecimal(reserved));
        return batch;
    }
    
    private BatchNumber createBatchWithDates(LocalDate productionDate, LocalDate expirationDate) {
        BatchNumber batch = new BatchNumber();
        batch.setProductionDate(productionDate);
        batch.setExpirationDate(expirationDate);
        return batch;
    }
    
    private boolean isValidBatchStatus(String status) {
        if (status == null || status.isEmpty()) {
            return false;
        }
        return status.equals("ACTIVE") || status.equals("EXPIRED") ||
               status.equals("QUARANTINED") || status.equals("CANCELLED");
    }
    
    private boolean isValidSourceType(String sourceType) {
        if (sourceType == null || sourceType.isEmpty()) {
            return false;
        }
        return sourceType.equals("PURCHASE") || sourceType.equals("PRODUCTION") ||
               sourceType.equals("SALE_RETURN");
    }
    
    private boolean isValidQualityStatus(String qualityStatus) {
        if (qualityStatus == null || qualityStatus.isEmpty()) {
            return false;
        }
        return qualityStatus.equals("NORMAL") || qualityStatus.equals("QUARANTINED") ||
               qualityStatus.equals("DEFECTIVE");
    }
    
    private boolean validateBatchQuantities(BatchNumber batch) {
        if (batch.getTotalQuantity() == null || batch.getAvailableQuantity() == null ||
            batch.getReservedQuantity() == null) {
            return false;
        }
        
        // 数量必须为非负数
        if (batch.getTotalQuantity().compareTo(BigDecimal.ZERO) < 0 ||
            batch.getAvailableQuantity().compareTo(BigDecimal.ZERO) < 0 ||
            batch.getReservedQuantity().compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        // 可用数量 + 预留数量 <= 总数量
        BigDecimal sum = batch.getAvailableQuantity().add(batch.getReservedQuantity());
        return batch.getTotalQuantity().compareTo(sum) >= 0;
    }
    
    private boolean validateBatchDates(BatchNumber batch) {
        // 如果生产日期不为null，不能是未来日期
        if (batch.getProductionDate() != null && 
            batch.getProductionDate().isAfter(LocalDate.now())) {
            return false;
        }
        
        // 如果有效期不为null，必须晚于生产日期（如果生产日期存在）
        if (batch.getExpirationDate() != null) {
            if (batch.getProductionDate() != null && 
                batch.getExpirationDate().isBefore(batch.getProductionDate())) {
                return false;
            }
            
            // 有效期不能早于当前日期（已过期）
            if (batch.getExpirationDate().isBefore(LocalDate.now())) {
                return false;
            }
        }
        
        return true;
    }
    
    private long calculateRemainingDays(BatchNumber batch) {
        if (batch.getExpirationDate() == null) {
            return Long.MAX_VALUE; // 无有效期限制
        }
        return java.time.temporal.ChronoUnit.DAYS.between(
            LocalDate.now(), batch.getExpirationDate());
    }
    
    private boolean isBatchExpiring(BatchNumber batch, int warningDays) {
        if (batch.getExpirationDate() == null) {
            return false; // 无有效期的批次不会过期
        }
        long remainingDays = calculateRemainingDays(batch);
        return remainingDays <= warningDays && remainingDays >= 0;
    }
    
    private boolean isBatchAvailable(BatchNumber batch) {
        // 批次必须活跃
        if (!"ACTIVE".equals(batch.getBatchStatus())) {
            return false;
        }
        
        // 批次不能过期
        if (batch.getExpirationDate() != null && 
            batch.getExpirationDate().isBefore(LocalDate.now())) {
            return false;
        }
        
        // 必须有可用库存
        if (batch.getAvailableQuantity() == null || 
            batch.getAvailableQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        return true;
    }
}