package cn.aiedge.erp.batchsn.service;

import cn.aiedge.erp.batchsn.entity.BatchNumber;
import cn.aiedge.erp.batchsn.enums.BatchStatusEnum;
import cn.aiedge.erp.batchsn.mapper.BatchFlowRecordMapper;
import cn.aiedge.erp.batchsn.mapper.BatchNumberMapper;
import cn.aiedge.erp.batchsn.service.impl.BatchNumberServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 批次号服务实现单元测试
 * 
 * @author team-member
 * @date 2026-05-05
 */
@ExtendWith(MockitoExtension.class)
class BatchNumberServiceImplTest {
    
    @Mock
    private BatchNumberMapper batchNumberMapper;
    
    @Mock
    private BatchFlowRecordMapper batchFlowRecordMapper;
    
    @InjectMocks
    private BatchNumberServiceImpl batchNumberService;
    
    private BatchNumber testBatch;
    private LocalDateTime now;
    
    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        testBatch = createTestBatch();
    }
    
    private BatchNumber createTestBatch() {
        BatchNumber batch = new BatchNumber();
        batch.setId(1L);
        batch.setBatchNo("B202605051001");
        batch.setProductId(1001L);
        batch.setProductCode("P001");
        batch.setProductName("测试产品");
        batch.setSpecification("标准规格");
        batch.setUnit("件");
        batch.setProductionDate(LocalDate.now().minusDays(10));
        batch.setExpirationDate(LocalDate.now().plusDays(90));
        batch.setBatchStatus(BatchStatusEnum.ACTIVE.getCode());
        batch.setTotalQuantity(new BigDecimal("100.00"));
        batch.setAvailableQuantity(new BigDecimal("80.00"));
        batch.setReservedQuantity(new BigDecimal("20.00"));
        batch.setSourceType("PURCHASE");
        batch.setSourceRefNo("PO-20260505-001");
        batch.setWarehouseId(1L);
        batch.setWarehouseName("主仓库");
        batch.setLocationId(10L);
        batch.setQualityStatus("QUALIFIED");
        batch.setCreatedAt(now.minusDays(5));
        batch.setUpdatedAt(now.minusDays(2));
        batch.setIsDeleted(0);
        return batch;
    }
    
    @Test
    @DisplayName("创建批次 - 成功")
    void createBatch_Success() {
        // 准备测试数据
        BatchNumber inputBatch = createTestBatch();
        inputBatch.setId(null); // 确保ID为空
        inputBatch.setBatchNo(null); // 让系统自动生成批次号
        
        // 模拟Mapper行为
        when(batchNumberMapper.insert(any(BatchNumber.class))).thenReturn(1);
        
        // 执行测试
        BatchNumber result = batchNumberService.createBatch(inputBatch);
        
        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getBatchNo()); // 批次号应被自动生成
        assertEquals(BatchStatusEnum.ACTIVE.getCode(), result.getBatchStatus());
        assertEquals(BigDecimal.ZERO, result.getTotalQuantity());
        assertEquals(BigDecimal.ZERO, result.getAvailableQuantity());
        assertEquals(BigDecimal.ZERO, result.getReservedQuantity());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).insert(any(BatchNumber.class));
    }
    
    @Test
    @DisplayName("创建批次 - 使用自定义批次号")
    void createBatch_WithCustomBatchNo() {
        // 准备测试数据
        BatchNumber inputBatch = createTestBatch();
        inputBatch.setId(null);
        inputBatch.setBatchNo("CUSTOM-001"); // 自定义批次号
        
        // 模拟Mapper行为
        when(batchNumberMapper.insert(any(BatchNumber.class))).thenReturn(1);
        
        // 执行测试
        BatchNumber result = batchNumberService.createBatch(inputBatch);
        
        // 验证结果
        assertNotNull(result);
        assertEquals("CUSTOM-001", result.getBatchNo()); // 应使用自定义批次号
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).insert(any(BatchNumber.class));
    }
    
    @Test
    @DisplayName("查询批次详情 - 成功")
    void getBatchById_Success() {
        // 模拟Mapper行为
        when(batchNumberMapper.selectById(1L)).thenReturn(testBatch);
        
        // 执行测试
        BatchNumber result = batchNumberService.getBatchById(1L);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("B202605051001", result.getBatchNo());
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).selectById(1L);
    }
    
    @Test
    @DisplayName("查询批次详情 - 批次不存在")
    void getBatchById_NotFound() {
        // 模拟Mapper行为
        when(batchNumberMapper.selectById(999L)).thenReturn(null);
        
        // 执行测试
        BatchNumber result = batchNumberService.getBatchById(999L);
        
        // 验证结果
        assertNull(result);
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).selectById(999L);
    }
    
    @Test
    @DisplayName("更新批次信息 - 成功")
    void updateBatch_Success() {
        // 准备测试数据
        BatchNumber existingBatch = createTestBatch();
        BatchNumber updateBatch = createTestBatch();
        updateBatch.setProductName("更新后的产品名称");
        updateBatch.setTotalQuantity(new BigDecimal("150.00"));
        
        // 模拟Mapper行为
        when(batchNumberMapper.selectById(1L)).thenReturn(existingBatch);
        when(batchNumberMapper.updateById(any(BatchNumber.class))).thenReturn(1);
        when(batchNumberMapper.selectById(1L)).thenReturn(updateBatch);
        
        // 执行测试
        BatchNumber result = batchNumberService.updateBatch(1L, updateBatch);
        
        // 验证结果
        assertNotNull(result);
        assertEquals("更新后的产品名称", result.getProductName());
        assertEquals(new BigDecimal("150.00"), result.getTotalQuantity());
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).selectById(1L);
        verify(batchNumberMapper, times(1)).updateById(any(BatchNumber.class));
    }
    
    @Test
    @DisplayName("更新批次信息 - 批次不存在")
    void updateBatch_NotFound() {
        // 准备测试数据
        BatchNumber updateBatch = createTestBatch();
        
        // 模拟Mapper行为
        when(batchNumberMapper.selectById(999L)).thenReturn(null);
        
        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> batchNumberService.updateBatch(999L, updateBatch));
        
        assertEquals("批次不存在: id=999", exception.getMessage());
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).selectById(999L);
        verify(batchNumberMapper, never()).updateById(any(BatchNumber.class));
    }
    
    @Test
    @DisplayName("批次入库操作 - 新批次")
    void inbound_NewBatch() {
        // 准备测试数据
        BatchNumber inputBatch = createTestBatch();
        inputBatch.setId(null);
        
        // 模拟Mapper行为
        when(batchNumberMapper.selectById(anyLong())).thenReturn(null);
        when(batchNumberMapper.insert(any(BatchNumber.class))).thenReturn(1);
        
        // 执行测试
        BatchNumber result = batchNumberService.inbound(inputBatch, 1L, "主仓库", 10L);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getWarehouseId());
        assertEquals("主仓库", result.getWarehouseName());
        assertEquals(10L, result.getLocationId());
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).insert(any(BatchNumber.class));
        verify(batchFlowRecordMapper, times(1)).insert(any());
    }
    
    @Test
    @DisplayName("批次入库操作 - 现有批次数量增加")
    void inbound_ExistingBatch() {
        // 准备测试数据
        BatchNumber existingBatch = createTestBatch();
        existingBatch.setTotalQuantity(new BigDecimal("100.00"));
        existingBatch.setAvailableQuantity(new BigDecimal("100.00"));
        
        BatchNumber inputBatch = new BatchNumber();
        inputBatch.setId(1L);
        inputBatch.setTotalQuantity(new BigDecimal("50.00"));
        
        // 模拟Mapper行为
        when(batchNumberMapper.selectById(1L)).thenReturn(existingBatch);
        when(batchNumberMapper.updateById(any(BatchNumber.class))).thenReturn(1);
        
        // 执行测试
        BatchNumber result = batchNumberService.inbound(inputBatch, 1L, "主仓库", 10L);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(new BigDecimal("150.00"), result.getTotalQuantity());
        assertEquals(new BigDecimal("150.00"), result.getAvailableQuantity());
        assertEquals(1L, result.getWarehouseId());
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).selectById(1L);
        verify(batchNumberMapper, times(1)).updateById(any(BatchNumber.class));
        verify(batchFlowRecordMapper, times(1)).insert(any());
    }
    
    @Test
    @DisplayName("批次出库操作 - 成功")
    void outbound_Success() {
        // 准备测试数据
        BatchNumber existingBatch = createTestBatch();
        existingBatch.setTotalQuantity(new BigDecimal("100.00"));
        existingBatch.setAvailableQuantity(new BigDecimal("100.00"));
        
        // 模拟Mapper行为
        when(batchNumberMapper.selectById(1L)).thenReturn(existingBatch);
        when(batchNumberMapper.updateById(any(BatchNumber.class))).thenReturn(1);
        
        // 执行测试
        BatchNumber result = batchNumberService.outbound(1L, new BigDecimal("30.00"), 1L, 10L);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(new BigDecimal("70.00"), result.getTotalQuantity());
        assertEquals(new BigDecimal("70.00"), result.getAvailableQuantity());
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).selectById(1L);
        verify(batchNumberMapper, times(1)).updateById(any(BatchNumber.class));
        verify(batchFlowRecordMapper, times(1)).insert(any());
    }
    
    @Test
    @DisplayName("批次出库操作 - 库存不足")
    void outbound_InsufficientStock() {
        // 准备测试数据
        BatchNumber existingBatch = createTestBatch();
        existingBatch.setTotalQuantity(new BigDecimal("50.00"));
        existingBatch.setAvailableQuantity(new BigDecimal("50.00"));
        
        // 模拟Mapper行为
        when(batchNumberMapper.selectById(1L)).thenReturn(existingBatch);
        
        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> batchNumberService.outbound(1L, new BigDecimal("60.00"), 1L, 10L));
        
        assertTrue(exception.getMessage().contains("批次可用数量不足"));
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).selectById(1L);
        verify(batchNumberMapper, never()).updateById(any(BatchNumber.class));
        verify(batchFlowRecordMapper, never()).insert(any());
    }
    
    @Test
    @DisplayName("批次出库操作 - 批次不存在")
    void outbound_BatchNotFound() {
        // 模拟Mapper行为
        when(batchNumberMapper.selectById(999L)).thenReturn(null);
        
        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> batchNumberService.outbound(999L, new BigDecimal("10.00"), 1L, 10L));
        
        assertEquals("批次不存在: id=999", exception.getMessage());
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).selectById(999L);
        verify(batchNumberMapper, never()).updateById(any(BatchNumber.class));
        verify(batchFlowRecordMapper, never()).insert(any());
    }
    
    @Test
    @DisplayName("质检操作 - 成功")
    void qualityInspection_Success() {
        // 模拟Mapper行为
        when(batchNumberMapper.selectById(1L)).thenReturn(testBatch);
        when(batchNumberMapper.updateById(any(BatchNumber.class))).thenReturn(1);
        
        // 执行测试
        BatchNumber result = batchNumberService.qualityInspection(
            1L, "UNQUALIFIED", "inspector001", "质检员张三");
        
        // 验证结果
        assertNotNull(result);
        assertEquals("UNQUALIFIED", result.getQualityStatus());
        assertEquals("inspector001", result.getQualityInspectorId());
        assertEquals("质检员张三", result.getQualityInspectorName());
        assertNotNull(result.getQualityInspectionDate());
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).selectById(1L);
        verify(batchNumberMapper, times(1)).updateById(any(BatchNumber.class));
    }
    
    @Test
    @DisplayName("验证批次号唯一性 - 存在")
    void batchNoExists_True() {
        // 模拟Mapper行为
        when(batchNumberMapper.selectByBatchNo("B202605051001")).thenReturn(testBatch);
        
        // 执行测试
        boolean exists = batchNumberService.batchNoExists("B202605051001");
        
        // 验证结果
        assertTrue(exists);
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).selectByBatchNo("B202605051001");
    }
    
    @Test
    @DisplayName("验证批次号唯一性 - 不存在")
    void batchNoExists_False() {
        // 模拟Mapper行为
        when(batchNumberMapper.selectByBatchNo("NONEXISTENT")).thenReturn(null);
        
        // 执行测试
        boolean exists = batchNumberService.batchNoExists("NONEXISTENT");
        
        // 验证结果
        assertFalse(exists);
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).selectByBatchNo("NONEXISTENT");
    }
    
    @Test
    @DisplayName("检查批次是否临期 - 是")
    void isBatchExpiring_True() {
        // 准备测试数据
        BatchNumber batch = createTestBatch();
        batch.setExpirationDate(LocalDate.now().plusDays(5)); // 5天后过期
        
        // 执行测试
        boolean isExpiring = batchNumberService.isBatchExpiring(batch, 10); // 10天预警期
        
        // 验证结果
        assertTrue(isExpiring);
    }
    
    @Test
    @DisplayName("检查批次是否临期 - 否")
    void isBatchExpiring_False() {
        // 准备测试数据
        BatchNumber batch = createTestBatch();
        batch.setExpirationDate(LocalDate.now().plusDays(20)); // 20天后过期
        
        // 执行测试
        boolean isExpiring = batchNumberService.isBatchExpiring(batch, 10); // 10天预警期
        
        // 验证结果
        assertFalse(isExpiring);
    }
    
    @Test
    @DisplayName("检查批次是否临期 - 无过期日期")
    void isBatchExpiring_NoExpirationDate() {
        // 准备测试数据
        BatchNumber batch = createTestBatch();
        batch.setExpirationDate(null); // 无过期日期
        
        // 执行测试
        boolean isExpiring = batchNumberService.isBatchExpiring(batch, 10);
        
        // 验证结果
        assertFalse(isExpiring);
    }
    
    @Test
    @DisplayName("获取批次可用数量 - 成功")
    void getAvailableQuantity_Success() {
        // 准备测试数据
        BatchNumber batch = createTestBatch();
        batch.setAvailableQuantity(new BigDecimal("75.50"));
        
        // 模拟Mapper行为
        when(batchNumberMapper.selectById(1L)).thenReturn(batch);
        
        // 执行测试
        BigDecimal quantity = batchNumberService.getAvailableQuantity(1L);
        
        // 验证结果
        assertEquals(new BigDecimal("75.50"), quantity);
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).selectById(1L);
    }
    
    @Test
    @DisplayName("获取批次可用数量 - 批次不存在")
    void getAvailableQuantity_BatchNotFound() {
        // 模拟Mapper行为
        when(batchNumberMapper.selectById(999L)).thenReturn(null);
        
        // 执行测试
        BigDecimal quantity = batchNumberService.getAvailableQuantity(999L);
        
        // 验证结果
        assertEquals(BigDecimal.ZERO, quantity);
        
        // 验证方法调用
        verify(batchNumberMapper, times(1)).selectById(999L);
    }
}