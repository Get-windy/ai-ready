package cn.aiedge.erp.batch.repository;

import cn.aiedge.erp.batch.model.BatchRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BatchRecordRepository 单元测试
 * 
 * 测试覆盖：
 * 1. 基本的CRUD操作
 * 2. 自定义查询方法
 * 3. JPQL查询
 * 4. 数据统计查询
 */
@DataJpaTest
@ActiveProfiles("test")
public class BatchRecordRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private BatchRecordRepository batchRecordRepository;
    
    private BatchRecord testBatchRecord;
    private BatchRecord testBatchRecord2;
    
    @BeforeEach
    void setUp() {
        // 创建测试批次记录
        testBatchRecord = createBatchRecord(
            1L, "BATCH-20250505-001", 1001L, 2001L, 
            LocalDate.now().minusDays(30), LocalDate.now().plusDays(365),
            BatchRecord.BatchStatus.ACTIVE, BatchRecord.QualityGrade.QUALIFIED,
            1000, 800
        );
        
        testBatchRecord2 = createBatchRecord(
            2L, "BATCH-20250505-002", 1002L, 2002L,
            LocalDate.now().minusDays(60), LocalDate.now().plusDays(180),
            BatchRecord.BatchStatus.ACTIVE, BatchRecord.QualityGrade.PENDING_INSPECTION,
            500, 500
        );
        
        // 保存到测试数据库
        entityManager.persist(testBatchRecord);
        entityManager.persist(testBatchRecord2);
        entityManager.flush();
    }
    
    private BatchRecord createBatchRecord(
        Long id, String batchNumber, Long productId, Long supplierId,
        LocalDate productionDate, LocalDate expiryDate,
        BatchRecord.BatchStatus status, BatchRecord.QualityGrade qualityGrade,
        Integer quantity, Integer remainingQuantity
    ) {
        BatchRecord batchRecord = new BatchRecord();
        batchRecord.setId(id);
        batchRecord.setBatchNumber(batchNumber);
        batchRecord.setProductId(productId);
        batchRecord.setSupplierId(supplierId);
        batchRecord.setProductionDate(productionDate);
        batchRecord.setExpiryDate(expiryDate);
        batchRecord.setStatus(status);
        batchRecord.setQualityGrade(qualityGrade);
        batchRecord.setQuantity(quantity);
        batchRecord.setRemainingQuantity(remainingQuantity);
        batchRecord.setWarehouseId(3001L);
        batchRecord.setLocation("A区-01架-01层");
        batchRecord.setRemark("测试批次记录");
        
        return batchRecord;
    }
    
    // ===================== 基本CRUD测试 =====================
    
    @Test
    void testFindById_Success() {
        // When
        Optional<BatchRecord> found = batchRecordRepository.findById(testBatchRecord.getId());
        
        // Then
        assertTrue(found.isPresent());
        assertEquals("BATCH-20250505-001", found.get().getBatchNumber());
        assertEquals(1001L, found.get().getProductId());
    }
    
    @Test
    void testFindById_NotFound() {
        // When
        Optional<BatchRecord> found = batchRecordRepository.findById(999L);
        
        // Then
        assertFalse(found.isPresent());
    }
    
    @Test
    void testSaveNewBatchRecord() {
        // Given
        BatchRecord newBatchRecord = createBatchRecord(
            null, "BATCH-20250505-003", 1003L, 2003L,
            LocalDate.now().minusDays(15), LocalDate.now().plusDays(730),
            BatchRecord.BatchStatus.ACTIVE, BatchRecord.QualityGrade.QUALIFIED,
            2000, 2000
        );
        
        // When
        BatchRecord savedBatch = batchRecordRepository.save(newBatchRecord);
        
        // Then
        assertNotNull(savedBatch.getId());
        assertEquals("BATCH-20250505-003", savedBatch.getBatchNumber());
        assertTrue(batchRecordRepository.existsById(savedBatch.getId()));
    }
    
    @Test
    void testUpdateBatchRecord() {
        // Given
        BatchRecord batchRecord = batchRecordRepository.findById(testBatchRecord.getId()).get();
        batchRecord.setRemark("更新后的备注");
        batchRecord.setLocation("B区-02架-03层");
        
        // When
        BatchRecord updatedBatch = batchRecordRepository.save(batchRecord);
        
        // Then
        assertEquals("更新后的备注", updatedBatch.getRemark());
        assertEquals("B区-02架-03层", updatedBatch.getLocation());
    }
    
    @Test
    void testDeleteBatchRecord() {
        // Given
        Long batchId = testBatchRecord.getId();
        
        // When
        batchRecordRepository.deleteById(batchId);
        
        // Then
        assertFalse(batchRecordRepository.existsById(batchId));
    }
    
    // ===================== 自定义查询方法测试 =====================
    
    @Test
    void testFindByBatchNumber_Success() {
        // When
        Optional<BatchRecord> found = batchRecordRepository.findByBatchNumber("BATCH-20250505-001");
        
        // Then
        assertTrue(found.isPresent());
        assertEquals(testBatchRecord.getId(), found.get().getId());
        assertEquals("BATCH-20250505-001", found.get().getBatchNumber());
    }
    
    @Test
    void testFindByBatchNumber_NotFound() {
        // When
        Optional<BatchRecord> found = batchRecordRepository.findByBatchNumber("NON-EXISTENT-BATCH");
        
        // Then
        assertFalse(found.isPresent());
    }
    
    @Test
    void testFindByProductId() {
        // Given
        // 创建另一个相同产品的批次
        BatchRecord sameProductBatch = createBatchRecord(
            null, "BATCH-20250505-004", 1001L, 2004L,
            LocalDate.now().minusDays(45), LocalDate.now().plusDays(270),
            BatchRecord.BatchStatus.ACTIVE, BatchRecord.QualityGrade.QUALIFIED,
            1500, 1500
        );
        batchRecordRepository.save(sameProductBatch);
        
        // When
        List<BatchRecord> batches = batchRecordRepository.findByProductId(1001L);
        
        // Then
        assertNotNull(batches);
        assertEquals(2, batches.size());
        assertTrue(batches.stream().allMatch(b -> b.getProductId().equals(1001L)));
    }
    
    @Test
    void testFindBySupplierId() {
        // When
        List<BatchRecord> batches = batchRecordRepository.findBySupplierId(2001L);
        
        // Then
        assertNotNull(batches);
        assertEquals(1, batches.size());
        assertEquals(2001L, batches.get(0).getSupplierId());
    }
    
    @Test
    void testFindByStatus() {
        // Given
        // 创建不同状态的批次
        BatchRecord expiredBatch = createBatchRecord(
            null, "BATCH-20250505-EXPIRED", 1004L, 2004L,
            LocalDate.now().minusDays(365), LocalDate.now().minusDays(1),
            BatchRecord.BatchStatus.EXPIRED, BatchRecord.QualityGrade.REJECTED,
            500, 0
        );
        batchRecordRepository.save(expiredBatch);
        
        // When
        List<BatchRecord> activeBatches = batchRecordRepository.findByStatus(BatchRecord.BatchStatus.ACTIVE);
        List<BatchRecord> expiredBatches = batchRecordRepository.findByStatus(BatchRecord.BatchStatus.EXPIRED);
        
        // Then
        assertNotNull(activeBatches);
        assertEquals(2, activeBatches.size()); // testBatchRecord和testBatchRecord2
        
        assertNotNull(expiredBatches);
        assertEquals(1, expiredBatches.size());
        assertEquals("BATCH-20250505-EXPIRED", expiredBatches.get(0).getBatchNumber());
    }
    
    @Test
    void testFindByProductIdAndStatus() {
        // When
        List<BatchRecord> batches = batchRecordRepository.findByProductIdAndStatus(
            1001L, BatchRecord.BatchStatus.ACTIVE);
        
        // Then
        assertNotNull(batches);
        assertEquals(1, batches.size());
        assertEquals(1001L, batches.get(0).getProductId());
        assertEquals(BatchRecord.BatchStatus.ACTIVE, batches.get(0).getStatus());
    }
    
    @Test
    void testFindByProductIdAndQualityGrade() {
        // When
        List<BatchRecord> qualifiedBatches = batchRecordRepository.findByProductIdAndQualityGrade(
            1001L, BatchRecord.QualityGrade.QUALIFIED);
        List<BatchRecord> pendingBatches = batchRecordRepository.findByProductIdAndQualityGrade(
            1002L, BatchRecord.QualityGrade.PENDING_INSPECTION);
        
        // Then
        assertNotNull(qualifiedBatches);
        assertEquals(1, qualifiedBatches.size());
        assertEquals(BatchRecord.QualityGrade.QUALIFIED, qualifiedBatches.get(0).getQualityGrade());
        
        assertNotNull(pendingBatches);
        assertEquals(1, pendingBatches.size());
        assertEquals(BatchRecord.QualityGrade.PENDING_INSPECTION, pendingBatches.get(0).getQualityGrade());
    }
    
    @Test
    void testFindByWarehouseId() {
        // When
        List<BatchRecord> batches = batchRecordRepository.findByWarehouseId(3001L);
        
        // Then
        assertNotNull(batches);
        assertTrue(batches.size() >= 2); // 至少两个批次在同一个仓库
        assertTrue(batches.stream().allMatch(b -> b.getWarehouseId().equals(3001L)));
    }
    
    @Test
    void testFindByProductionDateBetween() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(90);
        LocalDate endDate = LocalDate.now().minusDays(1);
        
        // When
        List<BatchRecord> batches = batchRecordRepository.findByProductionDateBetween(startDate, endDate);
        
        // Then
        assertNotNull(batches);
        assertEquals(2, batches.size()); // 两个批次都在这个时间范围内生产
    }
    
    @Test
    void testFindByExpiryDateBetween() {
        // Given
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(400);
        
        // When
        List<BatchRecord> batches = batchRecordRepository.findByExpiryDateBetween(startDate, endDate);
        
        // Then
        assertNotNull(batches);
        assertTrue(batches.size() >= 1); // 至少有一个批次在这个有效期范围内
    }
    
    @Test
    void testFindByProductIdIn() {
        // Given
        List<Long> productIds = Arrays.asList(1001L, 1002L);
        
        // When
        List<BatchRecord> batches = batchRecordRepository.findByProductIdIn(productIds);
        
        // Then
        assertNotNull(batches);
        assertEquals(2, batches.size());
        assertTrue(batches.stream().allMatch(b -> productIds.contains(b.getProductId())));
    }
    
    // ===================== 存在性检查测试 =====================
    
    @Test
    void testExistsByBatchNumber_True() {
        // When
        boolean exists = batchRecordRepository.existsByBatchNumber("BATCH-20250505-001");
        
        // Then
        assertTrue(exists);
    }
    
    @Test
    void testExistsByBatchNumber_False() {
        // When
        boolean exists = batchRecordRepository.existsByBatchNumber("NON-EXISTENT-BATCH");
        
        // Then
        assertFalse(exists);
    }
    
    // ===================== 计数和统计测试 =====================
    
    @Test
    void testCountActiveBatchesByProduct() {
        // When
        long count = batchRecordRepository.countActiveBatchesByProduct(1001L);
        
        // Then
        assertEquals(1, count);
    }
    
    @Test
    void testSumRemainingQuantityByProduct() {
        // Given
        // 创建另一个相同产品的批次
        BatchRecord anotherBatch = createBatchRecord(
            null, "BATCH-20250505-005", 1001L, 2005L,
            LocalDate.now().minusDays(20), LocalDate.now().plusDays(200),
            BatchRecord.BatchStatus.ACTIVE, BatchRecord.QualityGrade.QUALIFIED,
            300, 300
        );
        batchRecordRepository.save(anotherBatch);
        
        // When
        Integer totalRemaining = batchRecordRepository.sumRemainingQuantityByProduct(1001L);
        
        // Then
        assertNotNull(totalRemaining);
        assertEquals(1100, totalRemaining); // 800 + 300
    }
    
    @Test
    void testSumRemainingQuantityByProduct_NoActiveBatches() {
        // When
        Integer totalRemaining = batchRecordRepository.sumRemainingQuantityByProduct(9999L);
        
        // Then
        assertEquals(0, totalRemaining);
    }
    
    // ===================== JPQL查询测试 =====================
    
    @Test
    void testFindNearExpiryBatches() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate expiryDate = today.plusDays(30); // 未来30天内过期
        
        // 创建一个即将过期的批次
        BatchRecord nearExpiryBatch = createBatchRecord(
            null, "BATCH-20250505-NEAR-EXPIRY", 1005L, 2005L,
            LocalDate.now().minusDays(180), LocalDate.now().plusDays(15), // 15天后过期
            BatchRecord.BatchStatus.ACTIVE, BatchRecord.QualityGrade.QUALIFIED,
            100, 100
        );
        batchRecordRepository.save(nearExpiryBatch);
        
        // When
        List<BatchRecord> nearExpiryBatches = batchRecordRepository.findNearExpiryBatches(today, expiryDate);
        
        // Then
        assertNotNull(nearExpiryBatches);
        assertTrue(nearExpiryBatches.size() >= 1);
    }
    
    @Test
    void testFindExpiredBatches() {
        // Given
        LocalDate today = LocalDate.now();
        
        // 创建一个已过期的批次
        BatchRecord expiredBatch = createBatchRecord(
            null, "BATCH-20250505-EXPIRED", 1006L, 2006L,
            LocalDate.now().minusDays(365), LocalDate.now().minusDays(1), // 昨天过期
            BatchRecord.BatchStatus.ACTIVE, BatchRecord.QualityGrade.REJECTED,
            50, 50
        );
        batchRecordRepository.save(expiredBatch);
        
        // When
        List<BatchRecord> expiredBatches = batchRecordRepository.findExpiredBatches(today);
        
        // Then
        assertNotNull(expiredBatches);
        assertTrue(expiredBatches.size() >= 1);
    }
    
    // ===================== 性能测试 =====================
    
    @Test
    void testFindAllPerformance() {
        // Given: 创建多个批次记录
        for (int i = 0; i < 50; i++) {
            BatchRecord batch = createBatchRecord(
                null, String.format("BATCH-PERF-%04d", i), 
                1000L + i, 2000L + i,
                LocalDate.now().minusDays(i), LocalDate.now().plusDays(365 - i),
                BatchRecord.BatchStatus.ACTIVE, BatchRecord.QualityGrade.QUALIFIED,
                100 + i, 100 + i
            );
            batchRecordRepository.save(batch);
        }
        
        // When
        List<BatchRecord> allBatches = batchRecordRepository.findAll();
        
        // Then
        assertNotNull(allBatches);
        assertTrue(allBatches.size() >= 52); // 2个初始批次 + 50个新批次
    }
    
    // ===================== 边界条件测试 =====================
    
    @Test
    void testEmptyResultQueries() {
        // When
        List<BatchRecord> noSupplierBatches = batchRecordRepository.findBySupplierId(9999L);
        List<BatchRecord> noProductBatches = batchRecordRepository.findByProductId(9999L);
        
        // Then
        assertNotNull(noSupplierBatches);
        assertTrue(noSupplierBatches.isEmpty());
        
        assertNotNull(noProductBatches);
        assertTrue(noProductBatches.isEmpty());
    }
    
    @Test
    void testNullHandling() {
        // Given: 创建一个没有供应商的批次
        BatchRecord noSupplierBatch = createBatchRecord(
            null, "BATCH-NO-SUPPLIER", 1007L, null,
            LocalDate.now().minusDays(10), LocalDate.now().plusDays(200),
            BatchRecord.BatchStatus.ACTIVE, BatchRecord.QualityGrade.QUALIFIED,
            200, 200
        );
        batchRecordRepository.save(noSupplierBatch);
        
        // When: 查询供应商为null的批次
        List<BatchRecord> batches = batchRecordRepository.findAll();
        
        // Then: 确认可以处理null值
        assertNotNull(batches);
        assertTrue(batches.stream().anyMatch(b -> b.getSupplierId() == null));
    }
}