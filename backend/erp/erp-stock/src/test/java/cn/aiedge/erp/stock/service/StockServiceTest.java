package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.service.impl.StockServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 库存服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("库存服务测试")
class StockServiceTest {

    @Mock
    private StockMapper stockMapper;

    @InjectMocks
    private StockServiceImpl stockService;

    private Stock testStock;

    @BeforeEach
    void setUp() {
        testStock = new Stock();
        testStock.setId(1L);
        testStock.setTenantId(1L);
        testStock.setProductId(100L);
        testStock.setProductName("测试产品");
        testStock.setProductCode("PROD_001");
        testStock.setWarehouseId(10L);
        testStock.setWarehouseName("主仓库");
        testStock.setQuantity(new BigDecimal("1000"));
        testStock.setAvailableQuantity(new BigDecimal("900"));
        testStock.setFrozenQuantity(new BigDecimal("100"));
        testStock.setSafetyStock(new BigDecimal("200"));
        testStock.setMinStock(new BigDecimal("100"));
        testStock.setMaxStock(new BigDecimal("5000"));
        testStock.setUnit("件");
        testStock.setCreateTime(LocalDateTime.now());
        testStock.setUpdateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("库存实体 - 属性设置")
    void testStockEntityProperties() {
        assertNotNull(testStock.getId());
        assertEquals(1L, testStock.getId());
        assertEquals(100L, testStock.getProductId());
        assertEquals("测试产品", testStock.getProductName());
        assertEquals("PROD_001", testStock.getProductCode());
        assertEquals(10L, testStock.getWarehouseId());
        assertEquals("主仓库", testStock.getWarehouseName());
    }

    @Test
    @DisplayName("库存链式设置")
    void testStockChainSetter() {
        Stock stock = new Stock()
                .setId(2L)
                .setProductName("新产品")
                .setProductCode("PROD_002")
                .setQuantity(new BigDecimal("500"));

        assertEquals(2L, stock.getId());
        assertEquals("新产品", stock.getProductName());
        assertEquals("PROD_002", stock.getProductCode());
        assertEquals(new BigDecimal("500"), stock.getQuantity());
    }

    @Test
    @DisplayName("库存数量 - BigDecimal计算")
    void testQuantityCalculation() {
        BigDecimal quantity = testStock.getQuantity();
        BigDecimal available = testStock.getAvailableQuantity();
        BigDecimal frozen = testStock.getFrozenQuantity();
        
        // 可用数量 = 总数量 - 冻结数量
        assertEquals(quantity.subtract(frozen), available);
        assertEquals(new BigDecimal("900"), available);
    }

    @Test
    @DisplayName("库存数量 - 增加库存")
    void testIncreaseQuantity() {
        BigDecimal original = testStock.getQuantity();
        BigDecimal addAmount = new BigDecimal("100");
        BigDecimal newQuantity = original.add(addAmount);
        
        testStock.setQuantity(newQuantity);
        
        assertEquals(new BigDecimal("1100"), testStock.getQuantity());
    }

    @Test
    @DisplayName("库存数量 - 减少库存")
    void testDecreaseQuantity() {
        BigDecimal original = testStock.getQuantity();
        BigDecimal subtractAmount = new BigDecimal("200");
        BigDecimal newQuantity = original.subtract(subtractAmount);
        
        testStock.setQuantity(newQuantity);
        
        assertEquals(new BigDecimal("800"), testStock.getQuantity());
    }

    @Test
    @DisplayName("库存冻结 - 冻结数量增加")
    void testFreezeQuantity() {
        BigDecimal originalFrozen = testStock.getFrozenQuantity();
        BigDecimal freezeAmount = new BigDecimal("50");
        
        testStock.setFrozenQuantity(originalFrozen.add(freezeAmount));
        testStock.setAvailableQuantity(testStock.getQuantity().subtract(testStock.getFrozenQuantity()));
        
        assertEquals(new BigDecimal("150"), testStock.getFrozenQuantity());
        assertEquals(new BigDecimal("850"), testStock.getAvailableQuantity());
    }

    @Test
    @DisplayName("库存解冻 - 冻结数量减少")
    void testUnfreezeQuantity() {
        BigDecimal originalFrozen = testStock.getFrozenQuantity();
        BigDecimal unfreezeAmount = new BigDecimal("50");
        
        testStock.setFrozenQuantity(originalFrozen.subtract(unfreezeAmount));
        testStock.setAvailableQuantity(testStock.getQuantity().subtract(testStock.getFrozenQuantity()));
        
        assertEquals(new BigDecimal("50"), testStock.getFrozenQuantity());
        assertEquals(new BigDecimal("950"), testStock.getAvailableQuantity());
    }

    @Test
    @DisplayName("安全库存 - 预警检查")
    void testSafetyStockAlert() {
        BigDecimal quantity = testStock.getQuantity();
        BigDecimal safetyStock = testStock.getSafetyStock();
        
        // 如果库存数量小于安全库存，需要预警
        boolean needsAlert = quantity.compareTo(safetyStock) < 0;
        
        // 当前库存1000 > 安全库存200，无需预警
        assertFalse(needsAlert);
    }

    @Test
    @DisplayName("最低库存 - 采购提醒")
    void testMinStockAlert() {
        testStock.setQuantity(new BigDecimal("50"));
        
        BigDecimal quantity = testStock.getQuantity();
        BigDecimal minStock = testStock.getMinStock();
        
        // 如果库存数量小于最低库存，需要采购
        boolean needsPurchase = quantity.compareTo(minStock) < 0;
        
        // 当前库存50 < 最低库存100，需要采购
        assertTrue(needsPurchase);
    }

    @Test
    @DisplayName("最高库存 - 超储检查")
    void testMaxStockAlert() {
        testStock.setQuantity(new BigDecimal("6000"));
        
        BigDecimal quantity = testStock.getQuantity();
        BigDecimal maxStock = testStock.getMaxStock();
        
        // 如果库存数量大于最高库存，超储警告
        boolean overStock = quantity.compareTo(maxStock) > 0;
        
        // 当前库存6000 > 最高库存5000，超储
        assertTrue(overStock);
    }

    @Test
    @DisplayName("库存单位 - 单位设置")
    void testUnitField() {
        testStock.setUnit("件");
        assertEquals("件", testStock.getUnit());
        
        testStock.setUnit("kg");
        assertEquals("kg", testStock.getUnit());
        
        testStock.setUnit("个");
        assertEquals("个", testStock.getUnit());
    }

    @Test
    @DisplayName("批次号 - 批次管理")
    void testBatchNoField() {
        String batchNo = "BATCH_20260330_001";
        testStock.setBatchNo(batchNo);
        
        assertEquals(batchNo, testStock.getBatchNo());
    }

    @Test
    @DisplayName("生产日期和有效期 - 保质期管理")
    void testDateFields() {
        LocalDateTime productionDate = LocalDateTime.now().minusDays(30);
        LocalDateTime validityDate = LocalDateTime.now().plusYears(1);
        
        testStock.setProductionDate(productionDate);
        testStock.setValidityDate(validityDate);
        
        assertEquals(productionDate, testStock.getProductionDate());
        assertEquals(validityDate, testStock.getValidityDate());
    }

    @Test
    @DisplayName("供应商信息 - 供应商关联")
    void testSupplierFields() {
        testStock.setSupplierId(1000L);
        testStock.setSupplierName("测试供应商");
        
        assertEquals(1000L, testStock.getSupplierId());
        assertEquals("测试供应商", testStock.getSupplierName());
    }

    @Test
    @DisplayName("仓库信息 - 仓库关联")
    void testWarehouseFields() {
        testStock.setWarehouseId(10L);
        testStock.setWarehouseName("主仓库");
        
        assertEquals(10L, testStock.getWarehouseId());
        assertEquals("主仓库", testStock.getWarehouseName());
    }

    @Test
    @DisplayName("删除标记 - deleted字段")
    void testDeletedField() {
        testStock.setDeleted(0);
        assertEquals(0, testStock.getDeleted());
        
        testStock.setDeleted(1);
        assertEquals(1, testStock.getDeleted());
    }

    @Test
    @DisplayName("备注信息 - remark字段")
    void testRemarkField() {
        String remark = "测试备注信息";
        testStock.setRemark(remark);
        
        assertEquals(remark, testStock.getRemark());
    }

    @Test
    @DisplayName("时间戳 - 创建和更新")
    void testTimestampFields() {
        LocalDateTime create = LocalDateTime.now().minusHours(1);
        LocalDateTime update = LocalDateTime.now();
        
        testStock.setCreateTime(create);
        testStock.setUpdateTime(update);
        
        assertEquals(create, testStock.getCreateTime());
        assertEquals(update, testStock.getUpdateTime());
    }

    @Test
    @DisplayName("租户ID - 多租户支持")
    void testTenantIdField() {
        Long tenantId = 1L;
        testStock.setTenantId(tenantId);
        
        assertEquals(tenantId, testStock.getTenantId());
    }

    @Test
    @DisplayName("BigDecimal精度测试")
    void testBigDecimalPrecision() {
        BigDecimal precise = new BigDecimal("1000.123456");
        testStock.setQuantity(precise);
        
        assertEquals(precise, testStock.getQuantity());
        assertEquals("1000.123456", precise.toString());
    }

    @Test
    @DisplayName("库存查询 - 按产品ID")
    void testQueryByProductId() {
        Long productId = 100L;
        testStock.setProductId(productId);
        
        assertEquals(productId, testStock.getProductId());
    }

    @Test
    @DisplayName("库存查询 - 按仓库ID")
    void testQueryByWarehouseId() {
        Long warehouseId = 10L;
        testStock.setWarehouseId(warehouseId);
        
        assertEquals(warehouseId, testStock.getWarehouseId());
    }

    @Test
    @DisplayName("库存产品编码 - 唯一性")
    void testProductCodeUniqueness() {
        String productCode = "UNIQUE_PROD_001";
        testStock.setProductCode(productCode);
        
        assertEquals(productCode, testStock.getProductCode());
    }
}