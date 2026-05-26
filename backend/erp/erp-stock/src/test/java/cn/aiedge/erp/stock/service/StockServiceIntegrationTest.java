package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.service.impl.StockServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存服务集成测试
 */
@SpringBootTest
@Transactional
public class StockServiceIntegrationTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private StockMapper stockMapper;

    @BeforeEach
    public void setUp() {
        // 清理测试数据
        stockMapper.delete(new QueryWrapper<Stock>().eq("product_id", 9999L));
        stockMapper.delete(new QueryWrapper<Stock>().eq("product_id", 8888L));
        stockMapper.delete(new QueryWrapper<Stock>().eq("product_id", 7777L));
    }

    @AfterEach
    public void tearDown() {
        // 清理测试数据
        stockMapper.delete(new QueryWrapper<Stock>().eq("product_id", 9999L));
        stockMapper.delete(new QueryWrapper<Stock>().eq("product_id", 8888L));
        stockMapper.delete(new QueryWrapper<Stock>().eq("product_id", 7777L));
    }

    @Test
    public void testCheckStockAlert_WithDefaultThreshold_ReturnsLowStockItems() {
        // 创建测试库存数据
        Stock stock1 = new Stock();
        stock1.setProductId(9999L);
        stock1.setWarehouseId(1L);
        stock1.setAvailableQuantity(new BigDecimal("5")); // 低于默认阈值10
        stock1.setQuantity(new BigDecimal("5"));
        stock1.setFrozenQuantity(BigDecimal.ZERO);
        stockMapper.insert(stock1);

        Stock stock2 = new Stock();
        stock2.setProductId(8888L);
        stock2.setWarehouseId(1L);
        stock2.setAvailableQuantity(new BigDecimal("15")); // 高于默认阈值10
        stock2.setQuantity(new BigDecimal("15"));
        stock2.setFrozenQuantity(BigDecimal.ZERO);
        stockMapper.insert(stock2);

        Stock stock3 = new Stock();
        stock3.setProductId(7777L);
        stock3.setWarehouseId(1L);
        stock3.setAvailableQuantity(new BigDecimal("8")); // 低于默认阈值10
        stock3.setQuantity(new BigDecimal("8"));
        stock3.setFrozenQuantity(BigDecimal.ZERO);
        stockMapper.insert(stock3);

        // 执行库存预警检查
        List<Stock> alertStocks = stockService.checkStockAlert();

        // 验证结果
        assertNotNull(alertStocks);
        assertEquals(2, alertStocks.size());
        
        // 验证返回的都是低于阈值的库存
        for (Stock stock : alertStocks) {
            assertTrue(stock.getAvailableQuantity().compareTo(new BigDecimal("10")) < 0);
        }
        
        // 验证特定的产品ID
        boolean hasProduct9999 = alertStocks.stream().anyMatch(s -> s.getProductId().equals(9999L));
        boolean hasProduct7777 = alertStocks.stream().anyMatch(s -> s.getProductId().equals(7777L));
        boolean hasProduct8888 = alertStocks.stream().anyMatch(s -> s.getProductId().equals(8888L));
        
        assertTrue(hasProduct9999);
        assertTrue(hasProduct7777);
        assertFalse(hasProduct8888);
    }

    @Test
    public void testCheckStockAlert_NoLowStock_ReturnsEmptyList() {
        // 创建高于阈值的库存数据
        Stock stock1 = new Stock();
        stock1.setProductId(9999L);
        stock1.setWarehouseId(1L);
        stock1.setAvailableQuantity(new BigDecimal("15")); // 高于默认阈值10
        stock1.setQuantity(new BigDecimal("15"));
        stock1.setFrozenQuantity(BigDecimal.ZERO);
        stockMapper.insert(stock1);

        Stock stock2 = new Stock();
        stock2.setProductId(8888L);
        stock2.setWarehouseId(1L);
        stock2.setAvailableQuantity(new BigDecimal("20")); // 高于默认阈值10
        stock2.setQuantity(new BigDecimal("20"));
        stock2.setFrozenQuantity(BigDecimal.ZERO);
        stockMapper.insert(stock2);

        // 执行库存预警检查
        List<Stock> alertStocks = stockService.checkStockAlert();

        // 验证结果
        assertNotNull(alertStocks);
        assertTrue(alertStocks.isEmpty());
    }
}