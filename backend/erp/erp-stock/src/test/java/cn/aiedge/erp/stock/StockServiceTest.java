package cn.aiedge.erp.stock;

import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.service.StockService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存管理模块测试类
 * 
 * @author qa-lead
 * @since 2026-04-29
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("库存管理模块测试")
public class StockServiceTest {

    @Autowired
    private StockService stockService;

    private static final Long TEST_PRODUCT_ID = 9999L;
    private static final Long TEST_WAREHOUSE_ID = 8888L;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        Stock existingStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        if (existingStock != null) {
            stockService.removeById(existingStock.getId());
        }
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        Stock existingStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        if (existingStock != null) {
            stockService.removeById(existingStock.getId());
        }
    }

    // ==================== TC001: 查询库存详情 - 正常情况 ====================
    @Test
    @Order(1)
    @DisplayName("TC001: 查询库存详情 - 正常情况")
    @Transactional
    @Rollback(false)
    void testGetStockDetail_Success() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, 
                new BigDecimal("100"), new BigDecimal("80"), new BigDecimal("20"));
        stockService.save(stock);

        // 执行测试
        Stock result = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);

        // 验证结果
        assertNotNull(result, "库存记录不应为空");
        assertEquals(TEST_PRODUCT_ID, result.getProductId(), "产品ID应匹配");
        assertEquals(TEST_WAREHOUSE_ID, result.getWarehouseId(), "仓库ID应匹配");
        assertEquals(new BigDecimal("100"), result.getQuantity(), "库存数量应为100");
        assertEquals(new BigDecimal("80"), result.getAvailableQuantity(), "可用数量应为80");
        assertEquals(new BigDecimal("20"), result.getFrozenQuantity(), "冻结数量应为20");
    }

    // ==================== TC002: 查询库存详情 - 不存在的库存记录 ====================
    @Test
    @Order(2)
    @DisplayName("TC002: 查询库存详情 - 不存在的库存记录")
    void testGetStockDetail_NotFound() {
        // 执行测试
        Stock result = stockService.getStockDetail(99999L, 99999L);

        // 验证结果
        assertNull(result, "不存在的库存记录应返回null");
    }

    // ==================== TC003: 库存增加 - 新库存记录 ====================
    @Test
    @Order(3)
    @DisplayName("TC003: 库存增加 - 新库存记录")
    @Transactional
    @Rollback(false)
    void testIncreaseStock_NewRecord() {
        // 确保库存记录不存在
        Stock existingStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertNull(existingStock, "测试前库存记录不应存在");

        // 执行测试
        boolean result = stockService.increaseStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("50"));

        // 验证结果
        assertTrue(result, "增加库存应成功");
        
        Stock stock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertNotNull(stock, "库存记录应已创建");
        assertEquals(new BigDecimal("50"), stock.getQuantity(), "库存数量应为50");
        assertEquals(new BigDecimal("50"), stock.getAvailableQuantity(), "可用数量应为50");
        assertEquals(new BigDecimal("0"), stock.getFrozenQuantity(), "冻结数量应为0");
    }

    // ==================== TC004: 库存增加 - 已有库存记录 ====================
    @Test
    @Order(4)
    @DisplayName("TC004: 库存增加 - 已有库存记录")
    @Transactional
    @Rollback(false)
    void testIncreaseStock_ExistingRecord() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("80"), new BigDecimal("20"));
        stockService.save(stock);

        // 执行测试
        boolean result = stockService.increaseStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("30"));

        // 验证结果
        assertTrue(result, "增加库存应成功");
        
        Stock updatedStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertEquals(new BigDecimal("130"), updatedStock.getQuantity(), "库存数量应为130");
        assertEquals(new BigDecimal("110"), updatedStock.getAvailableQuantity(), "可用数量应为110");
        assertEquals(new BigDecimal("20"), updatedStock.getFrozenQuantity(), "冻结数量应为20");
    }

    // ==================== TC005: 库存减少 - 正常情况 ====================
    @Test
    @Order(5)
    @DisplayName("TC005: 库存减少 - 正常情况")
    @Transactional
    @Rollback(false)
    void testDecreaseStock_Success() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("80"), new BigDecimal("20"));
        stockService.save(stock);

        // 执行测试
        boolean result = stockService.decreaseStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("30"));

        // 验证结果
        assertTrue(result, "减少库存应成功");
        
        Stock updatedStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertEquals(new BigDecimal("70"), updatedStock.getQuantity(), "库存数量应为70");
        assertEquals(new BigDecimal("50"), updatedStock.getAvailableQuantity(), "可用数量应为50");
        assertEquals(new BigDecimal("20"), updatedStock.getFrozenQuantity(), "冻结数量应为20");
    }

    // ==================== TC006: 库存减少 - 库存不足 ====================
    @Test
    @Order(6)
    @DisplayName("TC006: 库存减少 - 库存不足")
    @Transactional
    @Rollback(false)
    void testDecreaseStock_Insufficient() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("80"), new BigDecimal("20"));
        stockService.save(stock);

        // 执行测试
        boolean result = stockService.decreaseStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("100"));

        // 验证结果
        assertFalse(result, "库存不足时应返回false");
        
        Stock unchangedStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertEquals(new BigDecimal("100"), unchangedStock.getQuantity(), "库存数量应不变");
        assertEquals(new BigDecimal("80"), unchangedStock.getAvailableQuantity(), "可用数量应不变");
    }

    // ==================== TC007: 库存冻结 - 正常情况 ====================
    @Test
    @Order(7)
    @DisplayName("TC007: 库存冻结 - 正常情况")
    @Transactional
    @Rollback(false)
    void testFreezeStock_Success() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("80"), new BigDecimal("20"));
        stockService.save(stock);

        // 执行测试
        boolean result = stockService.freezeStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("30"));

        // 验证结果
        assertTrue(result, "冻结库存应成功");
        
        Stock updatedStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertEquals(new BigDecimal("100"), updatedStock.getQuantity(), "库存数量应不变");
        assertEquals(new BigDecimal("50"), updatedStock.getAvailableQuantity(), "可用数量应为50");
        assertEquals(new BigDecimal("50"), updatedStock.getFrozenQuantity(), "冻结数量应为50");
    }

    // ==================== TC008: 库存冻结 - 可用库存不足 ====================
    @Test
    @Order(8)
    @DisplayName("TC008: 库存冻结 - 可用库存不足")
    @Transactional
    @Rollback(false)
    void testFreezeStock_Insufficient() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("80"), new BigDecimal("20"));
        stockService.save(stock);

        // 执行测试
        boolean result = stockService.freezeStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("100"));

        // 验证结果
        assertFalse(result, "可用库存不足时应返回false");
        
        Stock unchangedStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertEquals(new BigDecimal("80"), unchangedStock.getAvailableQuantity(), "可用数量应不变");
        assertEquals(new BigDecimal("20"), unchangedStock.getFrozenQuantity(), "冻结数量应不变");
    }

    // ==================== TC009: 库存解冻 - 正常情况 ====================
    @Test
    @Order(9)
    @DisplayName("TC009: 库存解冻 - 正常情况")
    @Transactional
    @Rollback(false)
    void testUnfreezeStock_Success() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("50"));
        stockService.save(stock);

        // 执行测试
        boolean result = stockService.unfreezeStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("20"));

        // 验证结果
        assertTrue(result, "解冻库存应成功");
        
        Stock updatedStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertEquals(new BigDecimal("100"), updatedStock.getQuantity(), "库存数量应不变");
        assertEquals(new BigDecimal("70"), updatedStock.getAvailableQuantity(), "可用数量应为70");
        assertEquals(new BigDecimal("30"), updatedStock.getFrozenQuantity(), "冻结数量应为30");
    }

    // ==================== TC010: 库存解冻 - 冻结库存不足 ====================
    @Test
    @Order(10)
    @DisplayName("TC010: 库存解冻 - 冻结库存不足")
    @Transactional
    @Rollback(false)
    void testUnfreezeStock_Insufficient() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("50"));
        stockService.save(stock);

        // 执行测试
        boolean result = stockService.unfreezeStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("100"));

        // 验证结果
        assertFalse(result, "冻结库存不足时应返回false");
        
        Stock unchangedStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertEquals(new BigDecimal("50"), unchangedStock.getAvailableQuantity(), "可用数量应不变");
        assertEquals(new BigDecimal("50"), unchangedStock.getFrozenQuantity(), "冻结数量应不变");
    }

    // ==================== TC011: 库存盘点 - 更新已有库存 ====================
    @Test
    @Order(11)
    @DisplayName("TC011: 库存盘点 - 更新已有库存")
    @Transactional
    @Rollback(false)
    void testCheckStock_UpdateExisting() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("80"), new BigDecimal("20"));
        stockService.save(stock);

        // 执行测试
        boolean result = stockService.checkStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("120"));

        // 验证结果
        assertTrue(result, "盘点库存应成功");
        
        Stock updatedStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertEquals(new BigDecimal("120"), updatedStock.getQuantity(), "库存数量应为120");
        assertEquals(new BigDecimal("100"), updatedStock.getAvailableQuantity(), "可用数量应为100");
        assertEquals(new BigDecimal("20"), updatedStock.getFrozenQuantity(), "冻结数量应为20");
    }

    // ==================== TC012: 库存盘点 - 创建新库存记录 ====================
    @Test
    @Order(12)
    @DisplayName("TC012: 库存盘点 - 创建新库存记录")
    @Transactional
    @Rollback(false)
    void testCheckStock_CreateNew() {
        // 确保库存记录不存在
        Stock existingStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertNull(existingStock, "测试前库存记录不应存在");

        // 执行测试
        boolean result = stockService.checkStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("200"));

        // 验证结果
        assertTrue(result, "盘点库存应成功");
        
        Stock stock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertNotNull(stock, "库存记录应已创建");
        assertEquals(new BigDecimal("200"), stock.getQuantity(), "库存数量应为200");
        assertEquals(new BigDecimal("200"), stock.getAvailableQuantity(), "可用数量应为200");
        assertEquals(new BigDecimal("0"), stock.getFrozenQuantity(), "冻结数量应为0");
    }

    // ==================== TC016: 库存增加 - 数量为0 ====================
    @Test
    @Order(16)
    @DisplayName("TC016: 库存增加 - 数量为0")
    @Transactional
    @Rollback(false)
    void testIncreaseStock_ZeroQuantity() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("0"));
        stockService.save(stock);

        // 执行测试
        boolean result = stockService.increaseStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("0"));

        // 验证结果
        assertFalse(result, "增加数量为0时应返回false");
        
        Stock unchangedStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertEquals(new BigDecimal("100"), unchangedStock.getQuantity(), "库存数量应不变");
    }

    // ==================== TC017: 库存增加 - 数量为负数 ====================
    @Test
    @Order(17)
    @DisplayName("TC017: 库存增加 - 数量为负数")
    @Transactional
    @Rollback(false)
    void testIncreaseStock_NegativeQuantity() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("0"));
        stockService.save(stock);

        // 执行测试
        boolean result = stockService.increaseStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("-10"));

        // 验证结果
        assertFalse(result, "增加数量为负数时应返回false");
        
        Stock unchangedStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertEquals(new BigDecimal("100"), unchangedStock.getQuantity(), "库存数量应不变");
    }

    // ==================== TC020: 库存盘点 - 数量为0 ====================
    @Test
    @Order(20)
    @DisplayName("TC020: 库存盘点 - 数量为0")
    @Transactional
    @Rollback(false)
    void testCheckStock_ZeroQuantity() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("0"));
        stockService.save(stock);

        // 执行测试
        boolean result = stockService.checkStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("0"));

        // 验证结果
        assertTrue(result, "盘点数量为0时应成功");
        
        Stock updatedStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertEquals(new BigDecimal("0"), updatedStock.getQuantity(), "库存数量应为0");
        assertEquals(new BigDecimal("0"), updatedStock.getAvailableQuantity(), "可用数量应为0");
    }

    // ==================== TC021: 库存盘点 - 数量为负数 ====================
    @Test
    @Order(21)
    @DisplayName("TC021: 库存盘点 - 数量为负数")
    @Transactional
    @Rollback(false)
    void testCheckStock_NegativeQuantity() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("0"));
        stockService.save(stock);

        // 执行测试
        boolean result = stockService.checkStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("-10"));

        // 验证结果
        assertFalse(result, "盘点数量为负数时应返回false");
        
        Stock unchangedStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertEquals(new BigDecimal("100"), unchangedStock.getQuantity(), "库存数量应不变");
    }

    // ==================== TC027: 传入null参数 - 数量为null ====================
    @Test
    @Order(27)
    @DisplayName("TC027: 传入null参数 - 数量为null")
    @Transactional
    @Rollback(false)
    void testIncreaseStock_NullQuantity() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("0"));
        stockService.save(stock);

        // 执行测试
        boolean result = stockService.increaseStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, null);

        // 验证结果
        assertFalse(result, "增加数量为null时应返回false");
        
        Stock unchangedStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertEquals(new BigDecimal("100"), unchangedStock.getQuantity(), "库存数量应不变");
    }

    // ==================== TC028: 并发库存操作 ====================
    @Test
    @Order(28)
    @DisplayName("TC028: 并发库存操作")
    @Transactional
    @Rollback(false)
    void testConcurrentStockOperations() throws InterruptedException {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("0"));
        stockService.save(stock);

        int threadCount = 10;
        int operationsPerThread = 10;
        BigDecimal decreaseAmount = new BigDecimal("1");

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        boolean result = stockService.decreaseStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, decreaseAmount);
                        if (result) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // 验证结果
        Stock finalStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertNotNull(finalStock, "库存记录应存在");
        
        // 100次减少操作，每次1个，总共应减少100个
        assertEquals(new BigDecimal("0"), finalStock.getQuantity(), 
                "并发操作后库存应为0，实际为: " + finalStock.getQuantity());
        assertEquals(successCount.get(), threadCount * operationsPerThread, 
                "所有操作都应成功");
    }

    // ==================== TC036: 库存增加后减少一致性 ====================
    @Test
    @Order(36)
    @DisplayName("TC036: 库存增加后减少一致性")
    @Transactional
    @Rollback(false)
    void testStockConsistency_IncreaseThenDecrease() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("0"));
        stockService.save(stock);

        // 执行增加和减少操作
        stockService.increaseStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("50"));
        stockService.decreaseStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("30"));

        // 验证结果
        Stock finalStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertEquals(new BigDecimal("120"), finalStock.getQuantity(), "库存数量应为120");
        assertEquals(new BigDecimal("120"), finalStock.getAvailableQuantity(), "可用数量应为120");
        assertEquals(new BigDecimal("0"), finalStock.getFrozenQuantity(), "冻结数量应为0");
    }

    // ==================== TC037: 冻结解冻一致性 ====================
    @Test
    @Order(37)
    @DisplayName("TC037: 冻结解冻一致性")
    @Transactional
    @Rollback(false)
    void testStockConsistency_FreezeThenUnfreeze() {
        // 准备测试数据
        Stock stock = createStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID,
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("0"));
        stockService.save(stock);

        // 执行冻结和解冻操作
        stockService.freezeStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("30"));
        stockService.unfreezeStock(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID, new BigDecimal("30"));

        // 验证结果
        Stock finalStock = stockService.getStockDetail(TEST_PRODUCT_ID, TEST_WAREHOUSE_ID);
        assertEquals(new BigDecimal("100"), finalStock.getQuantity(), "库存数量应为100");
        assertEquals(new BigDecimal("100"), finalStock.getAvailableQuantity(), "可用数量应为100");
        assertEquals(new BigDecimal("0"), finalStock.getFrozenQuantity(), "冻结数量应为0");
    }

    // ==================== TC038: 库存不变性检查 ====================
    @Test
    @Order(38)
    @DisplayName("TC038: 库存不变性检查")
    void testStockInvariant() {
        // 查询所有库存记录
        List<Stock> stockList = stockService.list();

        // 验证每条记录满足 quantity = availableQuantity + frozenQuantity
        for (Stock stock : stockList) {
            BigDecimal expectedQuantity = stock.getAvailableQuantity().add(stock.getFrozenQuantity());
            assertEquals(0, stock.getQuantity().compareTo(expectedQuantity),
                    "库存记录ID=" + stock.getId() + " 不满足不变性: " +
                    "quantity=" + stock.getQuantity() + 
                    ", availableQuantity=" + stock.getAvailableQuantity() +
                    ", frozenQuantity=" + stock.getFrozenQuantity());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建库存实体
     */
    private Stock createStock(Long productId, Long warehouseId, 
                             BigDecimal quantity, BigDecimal availableQuantity, BigDecimal frozenQuantity) {
        Stock stock = new Stock();
        stock.setProductId(productId);
        stock.setProductName("测试产品");
        stock.setProductCode("TEST" + productId);
        stock.setWarehouseId(warehouseId);
        stock.setWarehouseName("测试仓库");
        stock.setQuantity(quantity);
        stock.setAvailableQuantity(availableQuantity);
        stock.setFrozenQuantity(frozenQuantity);
        stock.setUnit("件");
        stock.setTenantId(1L);
        return stock;
    }
}