package cn.aiedge.erp.batch.service;

import cn.aiedge.erp.batch.model.BatchRecord;
import cn.aiedge.erp.batch.model.dto.CommonDTO;
import cn.aiedge.erp.batch.repository.BatchRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BatchRecordService 单元测试
 * 
 * 测试覆盖：
 * 1. 批次创建、更新、删除操作
 * 2. 批次查询功能
 * 3. 批次状态管理
 * 4. 批次质量等级管理
 * 5. 批次库存调整和转移
 */
@ExtendWith(MockitoExtension.class)
public class BatchRecordServiceTest {
    
    @Mock
    private BatchRecordRepository batchRecordRepository;
    
    private BatchRecordServiceImpl batchRecordService;
    
    private BatchRecord testBatchRecord;
    
    @BeforeEach
    void setUp() {
        testBatchRecord = createTestBatchRecord();
        batchRecordService = new BatchRecordServiceImpl(batchRecordRepository);
    }
    
    private BatchRecord createTestBatchRecord() {
        BatchRecord batchRecord = new BatchRecord();
        batchRecord.setId(1L);
        batchRecord.setBatchNumber("BATCH-20250505-001");
        batchRecord.setProductId(1001L);
        batchRecord.setProductionDate(LocalDate.now().minusDays(30));
        batchRecord.setExpiryDate(LocalDate.now().plusDays(365));
        batchRecord.setQuantity(1000);
        batchRecord.setRemainingQuantity(800);
        batchRecord.setStatus(BatchRecord.BatchStatus.ACTIVE);
        batchRecord.setQualityGrade(BatchRecord.QualityGrade.QUALIFIED);
        batchRecord.setSupplierId(2001L);
        batchRecord.setWarehouseId(3001L);
        batchRecord.setLocation("A区-01架-02层");
        batchRecord.setRemark("测试批次");
        batchRecord.setAttributes("{\"temperature\": \"2-8°C\", \"humidity\": \"60%\"}");
        return batchRecord;
    }
    
    // ===================== 创建批次记录测试 =====================
    
    @Test
    void testCreateBatchRecord_Success() {
        // Given
        when(batchRecordRepository.save(any(BatchRecord.class))).thenReturn(testBatchRecord);
        
        // When
        BatchRecord createdBatch = batchRecordService.createBatchRecord(testBatchRecord);
        
        // Then
        assertNotNull(createdBatch);
        assertEquals("BATCH-20250505-001", createdBatch.getBatchNumber());
        assertEquals(BatchRecord.BatchStatus.ACTIVE, createdBatch.getStatus());
        verify(batchRecordRepository, times(1)).save(testBatchRecord);
    }
    
    @Test
    void testCreateBatchRecord_WithNullBatchNumber() {
        // Given
        testBatchRecord.setBatchNumber(null);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.createBatchRecord(testBatchRecord);
        });
    }
    
    // ===================== 获取批次记录测试 =====================
    
    @Test
    void testGetBatchRecord_Success() {
        // Given
        when(batchRecordRepository.findById(1L)).thenReturn(Optional.of(testBatchRecord));
        
        // When
        BatchRecord foundBatch = batchRecordService.getBatchRecord(1L);
        
        // Then
        assertNotNull(foundBatch);
        assertEquals(1L, foundBatch.getId());
        verify(batchRecordRepository, times(1)).findById(1L);
    }
    
    @Test
    void testGetBatchRecord_NotFound() {
        // Given
        when(batchRecordRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.getBatchRecord(999L);
        });
    }
    
    @Test
    void testGetBatchRecordByBatchNumber_Success() {
        // Given
        when(batchRecordRepository.findByBatchNumber("BATCH-20250505-001"))
            .thenReturn(Optional.of(testBatchRecord));
        
        // When
        BatchRecord foundBatch = batchRecordService.getBatchRecordByBatchNumber("BATCH-20250505-001");
        
        // Then
        assertNotNull(foundBatch);
        assertEquals("BATCH-20250505-001", foundBatch.getBatchNumber());
        verify(batchRecordRepository, times(1)).findByBatchNumber("BATCH-20250505-001");
    }
    
    // ===================== 更新批次记录测试 =====================
    
    @Test
    void testUpdateBatchRecord_Success() {
        // Given
        BatchRecord updatedBatch = createTestBatchRecord();
        updatedBatch.setRemark("更新后的备注");
        updatedBatch.setLocation("B区-03架-01层");
        
        when(batchRecordRepository.findById(1L)).thenReturn(Optional.of(testBatchRecord));
        when(batchRecordRepository.save(any(BatchRecord.class))).thenReturn(updatedBatch);
        
        // When
        BatchRecord result = batchRecordService.updateBatchRecord(1L, updatedBatch);
        
        // Then
        assertNotNull(result);
        assertEquals("更新后的备注", result.getRemark());
        assertEquals("B区-03架-01层", result.getLocation());
        verify(batchRecordRepository, times(1)).findById(1L);
        verify(batchRecordRepository, times(1)).save(any(BatchRecord.class));
    }
    
    // ===================== 删除批次记录测试 =====================
    
    @Test
    void testDeleteBatchRecord_Success() {
        // Given
        when(batchRecordRepository.existsById(1L)).thenReturn(true);
        
        // When
        batchRecordService.deleteBatchRecord(1L);
        
        // Then
        verify(batchRecordRepository, times(1)).deleteById(1L);
    }
    
    @Test
    void testDeleteBatchRecord_NotFound() {
        // Given
        when(batchRecordRepository.existsById(999L)).thenReturn(false);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.deleteBatchRecord(999L);
        });
    }
    
    // ===================== 列表查询测试 =====================
    
    @Test
    void testListBatchRecords_Success() {
        // Given
        List<BatchRecord> batchRecords = Arrays.asList(testBatchRecord, createTestBatchRecord());
        when(batchRecordRepository.findAll()).thenReturn(batchRecords);
        
        // When
        List<BatchRecord> result = batchRecordService.listBatchRecords();
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(batchRecordRepository, times(1)).findAll();
    }
    
    @Test
    void testPageBatchRecords_Success() {
        // Given
        CommonDTO.PageRequest pageRequest = new CommonDTO.PageRequest();
        pageRequest.setPage(1);
        pageRequest.setSize(10);
        
        List<BatchRecord> batchRecords = Arrays.asList(testBatchRecord);
        Page<BatchRecord> pageResult = new PageImpl<>(batchRecords, PageRequest.of(0, 10), 1);
        when(batchRecordRepository.findAll(any(PageRequest.class))).thenReturn(pageResult);
        
        // When
        Page<BatchRecord> result = batchRecordService.pageBatchRecords(pageRequest);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        verify(batchRecordRepository, times(1)).findAll(any(PageRequest.class));
    }
    
    // ===================== 查询特定条件批次测试 =====================
    
    @Test
    void testListBatchRecordsByProduct_Success() {
        // Given
        List<BatchRecord> batchRecords = Arrays.asList(testBatchRecord);
        when(batchRecordRepository.findByProductId(1001L)).thenReturn(batchRecords);
        
        // When
        List<BatchRecord> result = batchRecordService.listBatchRecordsByProduct(1001L);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(batchRecordRepository, times(1)).findByProductId(1001L);
    }
    
    @Test
    void testListBatchRecordsBySupplier_Success() {
        // Given
        List<BatchRecord> batchRecords = Arrays.asList(testBatchRecord);
        when(batchRecordRepository.findBySupplierId(2001L)).thenReturn(batchRecords);
        
        // When
        List<BatchRecord> result = batchRecordService.listBatchRecordsBySupplier(2001L);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(batchRecordRepository, times(1)).findBySupplierId(2001L);
    }
    
    // ===================== 批次状态管理测试 =====================
    
    @Test
    void testUpdateBatchStatus_Success() {
        // Given
        when(batchRecordRepository.findById(1L)).thenReturn(Optional.of(testBatchRecord));
        when(batchRecordRepository.save(any(BatchRecord.class))).thenReturn(testBatchRecord);
        
        // When
        BatchRecord result = batchRecordService.updateBatchStatus(1L, BatchRecord.BatchStatus.QUARANTINED);
        
        // Then
        assertNotNull(result);
        assertEquals(BatchRecord.BatchStatus.QUARANTINED, result.getStatus());
        verify(batchRecordRepository, times(1)).findById(1L);
        verify(batchRecordRepository, times(1)).save(any(BatchRecord.class));
    }
    
    @Test
    void testUpdateBatchQualityGrade_Success() {
        // Given
        when(batchRecordRepository.findById(1L)).thenReturn(Optional.of(testBatchRecord));
        when(batchRecordRepository.save(any(BatchRecord.class))).thenReturn(testBatchRecord);
        
        // When
        BatchRecord result = batchRecordService.updateBatchQualityGrade(1L, BatchRecord.QualityGrade.REJECTED);
        
        // Then
        assertNotNull(result);
        assertEquals(BatchRecord.QualityGrade.REJECTED, result.getQualityGrade());
        verify(batchRecordRepository, times(1)).findById(1L);
        verify(batchRecordRepository, times(1)).save(any(BatchRecord.class));
    }
    
    // ===================== 批次库存操作测试 =====================
    
    @Test
    void testAdjustBatchQuantity_Success() {
        // Given
        testBatchRecord.setRemainingQuantity(800);
        when(batchRecordRepository.findById(1L)).thenReturn(Optional.of(testBatchRecord));
        when(batchRecordRepository.save(any(BatchRecord.class))).thenReturn(testBatchRecord);
        
        // When
        BatchRecord result = batchRecordService.adjustBatchQuantity(1L, -100, "出库100件");
        
        // Then
        assertNotNull(result);
        assertEquals(700, result.getRemainingQuantity());
        verify(batchRecordRepository, times(1)).findById(1L);
        verify(batchRecordRepository, times(1)).save(any(BatchRecord.class));
    }
    
    @Test
    void testAdjustBatchQuantity_InsufficientQuantity() {
        // Given
        testBatchRecord.setRemainingQuantity(50);
        when(batchRecordRepository.findById(1L)).thenReturn(Optional.of(testBatchRecord));
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.adjustBatchQuantity(1L, -100, "尝试出库超过库存数量");
        });
    }
    
    @Test
    void testTransferBatch_Success() {
        // Given
        when(batchRecordRepository.findById(1L)).thenReturn(Optional.of(testBatchRecord));
        when(batchRecordRepository.save(any(BatchRecord.class))).thenReturn(testBatchRecord);
        
        // When
        BatchRecord result = batchRecordService.transferBatch(1L, 3002L, "C区-05架-03层", "仓库调整");
        
        // Then
        assertNotNull(result);
        assertEquals(3002L, result.getWarehouseId());
        assertEquals("C区-05架-03层", result.getLocation());
        verify(batchRecordRepository, times(1)).findById(1L);
        verify(batchRecordRepository, times(1)).save(any(BatchRecord.class));
    }
    
    // ===================== 批次号相关测试 =====================
    
    @Test
    void testIsBatchNumberExists_True() {
        // Given
        when(batchRecordRepository.existsByBatchNumber("BATCH-20250505-001")).thenReturn(true);
        
        // When
        boolean exists = batchRecordService.isBatchNumberExists("BATCH-20250505-001");
        
        // Then
        assertTrue(exists);
        verify(batchRecordRepository, times(1)).existsByBatchNumber("BATCH-20250505-001");
    }
    
    @Test
    void testIsBatchNumberExists_False() {
        // Given
        when(batchRecordRepository.existsByBatchNumber("NEW-BATCH-001")).thenReturn(false);
        
        // When
        boolean exists = batchRecordService.isBatchNumberExists("NEW-BATCH-001");
        
        // Then
        assertFalse(exists);
        verify(batchRecordRepository, times(1)).existsByBatchNumber("NEW-BATCH-001");
    }
    
    @Test
    void testGenerateBatchNumber() {
        // Given
        Long productId = 1001L;
        LocalDate productionDate = LocalDate.of(2025, 5, 5);
        
        // When
        String batchNumber = batchRecordService.generateBatchNumber(productId, productionDate);
        
        // Then
        assertNotNull(batchNumber);
        assertTrue(batchNumber.startsWith("BATCH-"));
        assertTrue(batchNumber.contains("20250505"));
        assertTrue(batchNumber.contains("1001"));
    }
    
    // ===================== 异常情况测试 =====================
    
    @Test
    void testCreateBatchRecord_DuplicateBatchNumber() {
        // Given
        when(batchRecordRepository.existsByBatchNumber("BATCH-20250505-001")).thenReturn(true);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.createBatchRecord(testBatchRecord);
        });
    }
    
    @Test
    void testUpdateBatchRecord_InvalidBatch() {
        // Given
        testBatchRecord.setStatus(BatchRecord.BatchStatus.CANCELLED);
        when(batchRecordRepository.findById(1L)).thenReturn(Optional.of(testBatchRecord));
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.updateBatchRecord(1L, testBatchRecord);
        });
    }
    
    @Test
    void testUpdateBatchStatus_InvalidTransition() {
        // Given
        testBatchRecord.setStatus(BatchRecord.BatchStatus.CONSUMED);
        when(batchRecordRepository.findById(1L)).thenReturn(Optional.of(testBatchRecord));
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.updateBatchStatus(1L, BatchRecord.BatchStatus.ACTIVE);
        });
    }
}