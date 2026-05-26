package cn.aiedge.erp.batchsn.service;

import cn.aiedge.erp.batchsn.entity.BatchNumber;
import cn.aiedge.erp.batchsn.enums.BatchStatusEnum;
import cn.aiedge.erp.batchsn.mapper.BatchNumberMapper;
import cn.aiedge.erp.batchsn.service.impl.BatchNumberServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 批次号服务单元测试
 * 
 * @author team-member
 * @date 2026-05-05
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("批次号服务单元测试")
class BatchNumberServiceTest {
    
    @Mock
    private BatchNumberMapper batchNumberMapper;
    
    @Mock
    private BatchNumberService batchNumberService;
    
    @InjectMocks
    private BatchNumberServiceImpl batchNumberServiceImpl;
    
    private BatchNumber testBatch;
    
    @BeforeEach
    void setUp() {
        testBatch = new BatchNumber();
        testBatch.setId(1L);
        testBatch.setBatchNo("B202605050001");
        testBatch.setProductCode("P001");
        testBatch.setProductName("测试产品");
        testBatch.setBatchStatus(BatchStatusEnum.ACTIVE.getCode());
        testBatch.setTotalQuantity(new BigDecimal("100.00"));
        testBatch.setAvailableQuantity(new BigDecimal("100.00"));
        testBatch.setReservedQuantity(BigDecimal.ZERO);
        testBatch.setProductionDate(LocalDate.now());
        testBatch.setExpirationDate(LocalDate.now().plusYears(1));
        testBatch.setWarehouseId(1L);
        testBatch.setWarehouseName("主仓库");
        testBatch.setCreatedAt(LocalDateTime.now());
        testBatch.setUpdatedAt(LocalDateTime.now());
    }
    
    @Test
    @DisplayName("创建批次 - 成功")
    void createBatch_Success() {
        // 设置
        when(batchNumberMapper.insert(any(BatchNumber.class))).thenReturn(1);
        when(batchNumberMapper.selectById(anyLong())).thenReturn(testBatch);
        
        // 执行
        BatchNumber result = batchNumberServiceImpl.createBatch(testBatch);
        
        // 验证
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getBatchNo()).isEqualTo("B202605050001");
        verify(batchNumberMapper, times(1)).insert(any(BatchNumber.class));
    }
    
    @Test
    @DisplayName("创建批次 - 自动生成批次号")
    void createBatch_AutoGenerateBatchNo() {
        // 设置：批次号为null，应该自动生成
        testBatch.setBatchNo(null);
        when(batchNumberMapper.insert(any(BatchNumber.class))).thenReturn(1);
        when(batchNumberMapper.selectById(anyLong())).thenReturn(testBatch);
        
        // 执行
        BatchNumber result = batchNumberServiceImpl.createBatch(testBatch);
        
        // 验证：批次号应该被自动生成
        assertThat(result.getBatchNo()).isNotNull();
        assertThat(result.getBatchNo()).startsWith("B");
        assertThat(result.getBatchNo().length()).isGreaterThan(5);
    }
    
    @Test
    @DisplayName("查询批次详情 - 成功")
    void getBatchById_Success() {
        // 设置
        when(batchNumberMapper.selectById(1L)).thenReturn(testBatch);
        
        // 执行
        BatchNumber result = batchNumberServiceImpl.getBatchById(1L);
        
        // 验证
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProductCode()).isEqualTo("P001");
        verify(batchNumberMapper, times(1)).selectById(1L);
    }
    
    @Test
    @DisplayName("查询批次详情 - 批次不存在")
    void getBatchById_NotFound() {
        // 设置
        when(batchNumberMapper.selectById(999L)).thenReturn(null);
        
        // 执行
        BatchNumber result = batchNumberServiceImpl.getBatchById(999L);
        
        // 验证
        assertThat(result).isNull();
        verify(batchNumberMapper, times(1)).selectById(999L);
    }
    
    @Test
    @DisplayName("批次出库 - 成功")
    void outbound_Success() {
        // 设置
        when(batchNumberMapper.selectById(1L)).thenReturn(testBatch);
        when(batchNumberMapper.updateById(any(BatchNumber.class))).thenReturn(1);
        
        // 执行：出库50个
        BigDecimal outboundQuantity = new BigDecimal("50.00");
        BatchNumber result = batchNumberServiceImpl.outbound(1L, outboundQuantity, 1L, 1L);
        
        // 验证
        assertThat(result).isNotNull();
        assertThat(result.getTotalQuantity()).isEqualTo(new BigDecimal("50.00"));
        assertThat(result.getAvailableQuantity()).isEqualTo(new BigDecimal("50.00"));
        verify(batchNumberMapper, times(1)).updateById(any(BatchNumber.class));
    }
    
    @Test
    @DisplayName("批次出库 - 库存不足")
    void outbound_InsufficientStock() {
        // 设置：只有100个库存，尝试出库150个
        when(batchNumberMapper.selectById(1L)).thenReturn(testBatch);
        
        // 执行 & 验证
        BigDecimal outboundQuantity = new BigDecimal("150.00");
        assertThatThrownBy(() -> 
            batchNumberServiceImpl.outbound(1L, outboundQuantity, 1L, 1L)
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("批次可用数量不足");
        
        verify(batchNumberMapper, never()).updateById(any(BatchNumber.class));
    }
    
    @Test
    @DisplayName("批次质检 - 成功")
    void qualityInspection_Success() {
        // 设置
        when(batchNumberMapper.selectById(1L)).thenReturn(testBatch);
        when(batchNumberMapper.updateById(any(BatchNumber.class))).thenReturn(1);
        
        // 执行
        BatchNumber result = batchNumberServiceImpl.qualityInspection(
            1L, "PASS", "insp001", "质检员张三");
        
        // 验证
        assertThat(result).isNotNull();
        assertThat(result.getQualityStatus()).isEqualTo("PASS");
        assertThat(result.getQualityInspectorId()).isEqualTo("insp001");
        assertThat(result.getQualityInspectorName()).isEqualTo("质检员张三");
        verify(batchNumberMapper, times(1)).updateById(any(BatchNumber.class));
    }
    
    @Test
    @DisplayName("验证批次号唯一性 - 存在")
    void batchNoExists_Exists() {
        // 设置
        when(batchNumberMapper.selectByBatchNo("B202605050001")).thenReturn(testBatch);
        
        // 执行
        boolean exists = batchNumberServiceImpl.batchNoExists("B202605050001");
        
        // 验证
        assertThat(exists).isTrue();
        verify(batchNumberMapper, times(1)).selectByBatchNo("B202605050001");
    }
    
    @Test
    @DisplayName("验证批次号唯一性 - 不存在")
    void batchNoExists_NotExists() {
        // 设置
        when(batchNumberMapper.selectByBatchNo("NON_EXISTING")).thenReturn(null);
        
        // 执行
        boolean exists = batchNumberServiceImpl.batchNoExists("NON_EXISTING");
        
        // 验证
        assertThat(exists).isFalse();
        verify(batchNumberMapper, times(1)).selectByBatchNo("NON_EXISTING");
    }
    
    @Test
    @DisplayName("检查批次是否临期 - 临期")
    void isBatchExpiring_Expiring() {
        // 设置：批次即将在5天内过期
        testBatch.setExpirationDate(LocalDate.now().plusDays(5));
        
        // 执行：检查10天预警
        boolean isExpiring = batchNumberServiceImpl.isBatchExpiring(testBatch, 10);
        
        // 验证
        assertThat(isExpiring).isTrue();
    }
    
    @Test
    @DisplayName("检查批次是否临期 - 未临期")
    void isBatchExpiring_NotExpiring() {
        // 设置：批次还有20天才过期
        testBatch.setExpirationDate(LocalDate.now().plusDays(20));
        
        // 执行：检查10天预警
        boolean isExpiring = batchNumberServiceImpl.isBatchExpiring(testBatch, 10);
        
        // 验证
        assertThat(isExpiring).isFalse();
    }
    
    @Test
    @DisplayName("查询可用数量 - 成功")
    void getAvailableQuantity_Success() {
        // 设置
        when(batchNumberMapper.selectById(1L)).thenReturn(testBatch);
        
        // 执行
        BigDecimal available = batchNumberServiceImpl.getAvailableQuantity(1L);
        
        // 验证
        assertThat(available).isEqualTo(new BigDecimal("100.00"));
        verify(batchNumberMapper, times(1)).selectById(1L);
    }
    
    @Test
    @DisplayName("查询可用数量 - 批次不存在")
    void getAvailableQuantity_BatchNotFound() {
        // 设置
        when(batchNumberMapper.selectById(999L)).thenReturn(null);
        
        // 执行
        BigDecimal available = batchNumberServiceImpl.getAvailableQuantity(999L);
        
        // 验证：批次不存在时返回0
        assertThat(available).isEqualTo(BigDecimal.ZERO);
        verify(batchNumberMapper, times(1)).selectById(999L);
    }
    
    @Test
    @DisplayName("批量更新批次状态 - 成功")
    void updateBatchStatus_Success() {
        // 设置
        List<Long> batchIds = Arrays.asList(1L, 2L, 3L);
        when(batchNumberMapper.updateBatchStatus(eq(batchIds), anyString(), anyString())).thenReturn(3);
        
        // 执行
        int updated = batchNumberServiceImpl.updateBatchStatus(batchIds, "EXPIRED");
        
        // 验证
        assertThat(updated).isEqualTo(3);
        verify(batchNumberMapper, times(1)).updateBatchStatus(eq(batchIds), eq("EXPIRED"), anyString());
    }
    
    @Test
    @DisplayName("批量更新批次状态 - 空列表")
    void updateBatchStatus_EmptyList() {
        // 执行
        int updated = batchNumberServiceImpl.updateBatchStatus(Collections.emptyList(), "EXPIRED");
        
        // 验证
        assertThat(updated).isEqualTo(0);
        verify(batchNumberMapper, never()).updateBatchStatus(any(), anyString(), anyString());
    }
}