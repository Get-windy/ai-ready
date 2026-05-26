package cn.aiedge.erp.batch.integration;

import cn.aiedge.erp.batch.model.BatchRecord;
import cn.aiedge.erp.batch.repository.BatchRecordRepository;
import cn.aiedge.erp.batch.service.BatchRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 批次管理集成测试
 * 
 * 测试覆盖完整的批次生命周期：
 * 1. 批次创建 → 库存调整 → 批次转移 → 状态更新 → 批次消耗
 * 2. 完整的业务流程验证
 * 3. 数据库事务和并发处理
 * 4. 错误恢复和数据一致性
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("integration-test")
public class BatchManagementIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("batch_test_db")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", 
                    () -> "org.hibernate.dialect.PostgreSQLDialect");
    }
    
    @Autowired
    private BatchRecordRepository batchRecordRepository;
    
    @Autowired
    private BatchRecordService batchRecordService;
    
    @BeforeEach
    void setUp() {
        batchRecordRepository.deleteAll();
    }
    
    // ===================== 完整批次生命周期测试 =====================
    
    @Test
    void testCompleteBatchLifecycle() {
        // 阶段1: 创建批次
        BatchRecord newBatch = createTestBatchRecord();
        BatchRecord createdBatch = batchRecordService.createBatchRecord(newBatch);
        
        assertNotNull(createdBatch.getId());
        assertEquals("BATCH-20250505-001", createdBatch.getBatchNumber());
        assertEquals(BatchRecord.BatchStatus.ACTIVE, createdBatch.getStatus());
        assertEquals(BatchRecord.QualityGrade.PENDING_INSPECTION, createdBatch.getQualityGrade());
        assertEquals(1000, createdBatch.getQuantity());
        assertEquals(1000, createdBatch.getRemainingQuantity()); // 初始剩余数量等于总量
        
        // 阶段2: 更新批次质量等级
        createdBatch = batchRecordService.updateBatchQualityGrade(
            createdBatch.getId(), BatchRecord.QualityGrade.QUALIFIED);
        assertEquals(BatchRecord.QualityGrade.QUALIFIED, createdBatch.getQualityGrade());
        
        // 阶段3: 库存调整（出库）
        createdBatch = batchRecordService.adjustBatchQuantity(
            createdBatch.getId(), -300, "销售出库300件");
        assertEquals(700, createdBatch.getRemainingQuantity());
        
        // 阶段4: 库存调整（入库）
        createdBatch = batchRecordService.adjustBatchQuantity(
            createdBatch.getId(), 50, "退货入库50件");
        assertEquals(750, createdBatch.getRemainingQuantity());
        
        // 阶段5: 批次转移
        createdBatch = batchRecordService.transferBatch(
            createdBatch.getId(), 3002L, "B区-03架-02层", "仓库调整");
        assertEquals(3002L, createdBatch.getWarehouseId());
        assertEquals("B区-03架-02层", createdBatch.getLocation());
        
        // 阶段6: 查询验证
        BatchRecord retrievedBatch = batchRecordService.getBatchRecord(createdBatch.getId());
        assertEquals("BATCH-20250505-001", retrievedBatch.getBatchNumber());
        assertEquals(750, retrievedBatch.getRemainingQuantity());
        assertEquals(3002L, retrievedBatch.getWarehouseId());
        
        // 阶段7: 列出批次
        List<BatchRecord> batches = batchRecordService.listBatchRecords();
        assertNotNull(batches);
        assertTrue(batches.size() >= 1);
        
        // 阶段8: 消耗所有库存
        createdBatch = batchRecordService.adjustBatchQuantity(
            createdBatch.getId(), -750, "完全出库");
        assertEquals(0, createdBatch.getRemainingQuantity());
        assertEquals(BatchRecord.BatchStatus.CONSUMED, createdBatch.getStatus());
        
        logTestResult("完整批次生命周期测试通过");
    }
    
    @Test
    void testBatchStatusTransitionWorkflow() {
        // 创建批次
        BatchRecord batch = createTestBatchRecord();
        BatchRecord createdBatch = batchRecordService.createBatchRecord(batch);
        
        // 转换为隔离状态（质量问题）
        createdBatch = batchRecordService.updateBatchStatus(
            createdBatch.getId(), BatchRecord.BatchStatus.QUARANTINED);
        assertEquals(BatchRecord.BatchStatus.QUARANTINED, createdBatch.getStatus());
        
        // 隔离批次不能进行库存调整
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.adjustBatchQuantity(createdBatch.getId(), -100, "尝试从隔离批次出库");
        });
        
        // 隔离批次不能转移
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.transferBatch(createdBatch.getId(), 3003L, "新位置", "尝试转移隔离批次");
        });
        
        // 解除隔离，转换为活动状态
        createdBatch = batchRecordService.updateBatchStatus(
            createdBatch.getId(), BatchRecord.BatchStatus.ACTIVE);
        assertEquals(BatchRecord.BatchStatus.ACTIVE, createdBatch.getStatus());
        
        // 现在可以进行操作
        createdBatch = batchRecordService.adjustBatchQuantity(
            createdBatch.getId(), -200, "从解除隔离批次出库");
        assertEquals(800, createdBatch.getRemainingQuantity());
        
        logTestResult("批次状态转换工作流测试通过");
    }
    
    @Test
    void testBatchExpiryManagement() {
        // 创建不同有效期的批次
        BatchRecord normalBatch = createTestBatchRecord();
        normalBatch.setBatchNumber("BATCH-NORMAL-001");
        normalBatch.setExpiryDate(LocalDate.now().plusDays(365));
        batchRecordService.createBatchRecord(normalBatch);
        
        BatchRecord nearExpiryBatch = createTestBatchRecord();
        nearExpiryBatch.setBatchNumber("BATCH-NEAR-EXPIRY-001");
        nearExpiryBatch.setExpiryDate(LocalDate.now().plusDays(15)); // 15天后过期
        batchRecordService.createBatchRecord(nearExpiryBatch);
        
        BatchRecord expiredBatch = createTestBatchRecord();
        expiredBatch.setBatchNumber("BATCH-EXPIRED-001");
        expiredBatch.setExpiryDate(LocalDate.now().minusDays(1)); // 已过期
        batchRecordService.createBatchRecord(expiredBatch);
        
        // 手动更新过期批次状态
        expiredBatch = batchRecordService.updateBatchStatus(
            expiredBatch.getId(), BatchRecord.BatchStatus.EXPIRED);
        
        // 查询即将过期批次
        List<BatchRecord> nearExpiryBatches = batchRecordService.listNearExpiryBatches(30);
        assertNotNull(nearExpiryBatches);
        assertTrue(nearExpiryBatches.stream()
            .anyMatch(b -> b.getBatchNumber().equals("BATCH-NEAR-EXPIRY-001")));
        
        // 查询已过期批次
        List<BatchRecord> expiredBatches = batchRecordService.listExpiredBatches();
        assertNotNull(expiredBatches);
        assertTrue(expiredBatches.stream()
            .anyMatch(b -> b.getBatchNumber().equals("BATCH-EXPIRED-001")));
        
        // 过期批次不能操作
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.adjustBatchQuantity(expiredBatch.getId(), -100, "尝试从过期批次出库");
        });
        
        logTestResult("批次有效期管理测试通过");
    }
    
    @Test
    void testBatchNumberGenerationAndValidation() {
        Long productId = 1001L;
        LocalDate productionDate = LocalDate.of(2025, 5, 5);
        
        // 生成批次号
        String batchNumber1 = batchRecordService.generateBatchNumber(productId, productionDate);
        assertNotNull(batchNumber1);
        assertTrue(batchNumber1.startsWith("BATCH-20250505-1001-"));
        
        // 创建批次
        BatchRecord batch1 = createTestBatchRecord();
        batch1.setBatchNumber(batchNumber1);
        batchRecordService.createBatchRecord(batch1);
        
        // 检查批次号存在
        boolean exists = batchRecordService.isBatchNumberExists(batchNumber1);
        assertTrue(exists);
        
        // 尝试创建重复批次号应该失败
        BatchRecord duplicateBatch = createTestBatchRecord();
        duplicateBatch.setBatchNumber(batchNumber1);
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.createBatchRecord(duplicateBatch);
        });
        
        // 生成另一个批次号
        String batchNumber2 = batchRecordService.generateBatchNumber(productId, productionDate);
        assertNotNull(batchNumber2);
        assertNotEquals(batchNumber1, batchNumber2);
        
        // 新批次号应该不存在
        exists = batchRecordService.isBatchNumberExists(batchNumber2);
        assertFalse(exists);
        
        logTestResult("批次号生成与验证测试通过");
    }
    
    @Test
    void testConcurrentBatchOperations() throws InterruptedException {
        // 创建测试批次
        BatchRecord batch = createTestBatchRecord();
        batch.setQuantity(1000);
        batch.setRemainingQuantity(1000);
        BatchRecord createdBatch = batchRecordService.createBatchRecord(batch);
        
        // 模拟并发出库操作
        int threadCount = 5;
        int outboundPerThread = 50;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < outboundPerThread; j++) {
                        batchRecordService.adjustBatchQuantity(
                            createdBatch.getId(), 
                            -1, 
                            String.format("线程%d-出库%d", threadId, j)
                        );
                    }
                } catch (Exception e) {
                    System.err.println("线程" + threadId + "执行失败: " + e.getMessage());
                }
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证最终库存
        BatchRecord finalBatch = batchRecordService.getBatchRecord(createdBatch.getId());
        int expectedRemaining = 1000 - (threadCount * outboundPerThread);
        assertEquals(expectedRemaining, finalBatch.getRemainingQuantity());
        
        // 如果库存为0，状态应该自动变为已消耗
        if (expectedRemaining == 0) {
            assertEquals(BatchRecord.BatchStatus.CONSUMED, finalBatch.getStatus());
        }
        
        logTestResult("并发批次操作测试通过");
    }
    
    @Test
    void testBatchStatisticsAndReporting() {
        // 创建多个批次用于统计
        for (int i = 1; i <= 10; i++) {
            BatchRecord batch = createTestBatchRecord();
            batch.setBatchNumber(String.format("BATCH-STATS-%03d", i));
            batch.setProductId(5000L + (i % 3)); // 3个不同产品
            batch.setSupplierId(6000L + (i % 2)); // 2个不同供应商
            batch.setQuantity(100 * i);
            batch.setRemainingQuantity(100 * i);
            batch.setStatus(i <= 8 ? BatchRecord.BatchStatus.ACTIVE : BatchRecord.BatchStatus.EXPIRED);
            batch.setQualityGrade(i <= 7 ? BatchRecord.QualityGrade.QUALIFIED : BatchRecord.QualityGrade.REJECTED);
            
            batchRecordService.createBatchRecord(batch);
        }
        
        // 测试各种查询
        List<BatchRecord> allBatches = batchRecordService.listBatchRecords();
        assertEquals(10, allBatches.size());
        
        // 按产品查询
        List<BatchRecord> product5000Batches = batchRecordService.listBatchRecordsByProduct(5000L);
        assertNotNull(product5000Batches);
        
        // 按供应商查询
        List<BatchRecord> supplier6000Batches = batchRecordService.listBatchRecordsBySupplier(6000L);
        assertNotNull(supplier6000Batches);
        
        // 按状态查询
        List<BatchRecord> activeBatches = batchRecordService.listBatchRecordsByStatus(BatchRecord.BatchStatus.ACTIVE);
        assertEquals(8, activeBatches.size());
        
        List<BatchRecord> expiredBatches = batchRecordService.listBatchRecordsByStatus(BatchRecord.BatchStatus.EXPIRED);
        assertEquals(2, expiredBatches.size());
        
        // 按质量等级查询
        List<BatchRecord> qualifiedBatches = batchRecordService.listBatchRecordsByQualityGrade(BatchRecord.QualityGrade.QUALIFIED);
        assertEquals(7, qualifiedBatches.size());
        
        List<BatchRecord> rejectedBatches = batchRecordService.listBatchRecordsByQualityGrade(BatchRecord.QualityGrade.REJECTED);
        assertEquals(3, rejectedBatches.size());
        
        logTestResult("批次统计与报告测试通过");
    }
    
    @Test
    void testErrorHandlingAndDataConsistency() {
        // 测试1: 无效批次号
        BatchRecord invalidBatch = createTestBatchRecord();
        invalidBatch.setBatchNumber(null);
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.createBatchRecord(invalidBatch);
        });
        
        // 测试2: 无效数量
        invalidBatch = createTestBatchRecord();
        invalidBatch.setQuantity(0);
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.createBatchRecord(invalidBatch);
        });
        
        // 测试3: 无效日期
        invalidBatch = createTestBatchRecord();
        invalidBatch.setExpiryDate(null);
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.createBatchRecord(invalidBatch);
        });
        
        // 测试4: 创建有效批次
        BatchRecord validBatch = createTestBatchRecord();
        BatchRecord createdBatch = batchRecordService.createBatchRecord(validBatch);
        
        // 测试5: 尝试更新不存在的批次
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.updateBatchRecord(99999L, validBatch);
        });
        
        // 测试6: 库存不足
        assertThrows(IllegalArgumentException.class, () -> {
            batchRecordService.adjustBatchQuantity(createdBatch.getId(), -2000, "尝试出库超过库存数量");
        });
        
        // 测试7: 验证数据一致性 - 创建后应该可以查询到
        BatchRecord retrievedBatch = batchRecordService.getBatchRecord(createdBatch.getId());
        assertNotNull(retrievedBatch);
        assertEquals(validBatch.getBatchNumber(), retrievedBatch.getBatchNumber());
        
        logTestResult("错误处理与数据一致性测试通过");
    }
    
    // ===================== 辅助方法 =====================
    
    private BatchRecord createTestBatchRecord() {
        BatchRecord batchRecord = new BatchRecord();
        batchRecord.setBatchNumber("BATCH-20250505-001");
        batchRecord.setProductId(1001L);
        batchRecord.setSupplierId(2001L);
        batchRecord.setProductionDate(LocalDate.now().minusDays(30));
        batchRecord.setExpiryDate(LocalDate.now().plusDays(365));
        batchRecord.setQuantity(1000);
        batchRecord.setRemainingQuantity(1000);
        batchRecord.setStatus(BatchRecord.BatchStatus.ACTIVE);
        batchRecord.setQualityGrade(BatchRecord.QualityGrade.PENDING_INSPECTION);
        batchRecord.setWarehouseId(3001L);
        batchRecord.setLocation("A区-01架-01层");
        batchRecord.setRemark("集成测试批次");
        batchRecord.setAttributes("{\"test\": true}");
        
        return batchRecord;
    }
    
    private void logTestResult(String message) {
        System.out.println("✅ " + message);
    }
}