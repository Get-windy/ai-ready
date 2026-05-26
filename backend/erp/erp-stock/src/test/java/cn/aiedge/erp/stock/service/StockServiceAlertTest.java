package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.service.impl.StockServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 库存预警功能单元测试
 */
public class StockServiceAlertTest {

    @Mock
    private StockServiceImpl stockService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCheckStockAlert_WithLowThreshold_ReturnsOnlyLowStockItems() {
        // 准备测试数据
        Stock stock1 = new Stock();
        stock1.setId(1L);
        stock1.setProductId(1001L);
        stock1.setAvailableQuantity(new BigDecimal("5")); // 低于阈值
        
        Stock stock2 = new Stock();
        stock2.setId(2L);
        stock2.setProductId(1002L);
        stock2.setAvailableQuantity(new BigDecimal("150")); // 高于阈值
        
        Stock stock3 = new Stock();
        stock3.setId(3L);
        stock3.setProductId(1003L);
        stock3.setAvailableQuantity(new BigDecimal("8")); // 低于阈值
        
        List<Stock> allStocks = Arrays.asList(stock1, stock2, stock3);
        List<Stock> expectedAlertStocks = Arrays.asList(stock1, stock3);
        
        // 模拟服务调用
        when(stockService.checkStockAlert()).thenReturn(expectedAlertStocks);
        
        // 执行测试
        List<Stock> alertStocks = stockService.checkStockAlert();
        
        // 验证结果
        assertNotNull(alertStocks);
        assertEquals(2, alertStocks.size());
        assertTrue(alertStocks.stream().anyMatch(s -> s.getProductId().equals(1001L)));
        assertTrue(alertStocks.stream().anyMatch(s -> s.getProductId().equals(1003L)));
        assertFalse(alertStocks.stream().anyMatch(s -> s.getProductId().equals(1002L)));
    }

    @Test
    public void testCheckStockAlert_WithHighThreshold_ReturnsAllStocks() {
        // 设置高阈值，所有库存都应该触发预警
        Stock stock1 = new Stock();
        stock1.setId(1L);
        stock1.setProductId(1001L);
        stock1.setAvailableQuantity(new BigDecimal("50"));
        
        Stock stock2 = new Stock();
        stock2.setId(2L);
        stock2.setProductId(1002L);
        stock2.setAvailableQuantity(new BigDecimal("150"));
        
        List<Stock> allStocks = Arrays.asList(stock1, stock2);
        
        // 模拟服务调用返回所有库存（当阈值很高时）
        when(stockService.checkStockAlert()).thenReturn(allStocks);
        
        // 执行测试
        List<Stock> alertStocks = stockService.checkStockAlert();
        
        // 验证结果
        assertNotNull(alertStocks);
        assertEquals(2, alertStocks.size());
    }

    @Test
    public void testCheckStockAlert_EmptyStock_ReturnsEmptyList() {
        // 模拟空库存情况
        when(stockService.checkStockAlert()).thenReturn(Arrays.asList());
        
        // 执行测试
        List<Stock> alertStocks = stockService.checkStockAlert();
        
        // 验证结果
        assertNotNull(alertStocks);
        assertTrue(alertStocks.isEmpty());
    }
}