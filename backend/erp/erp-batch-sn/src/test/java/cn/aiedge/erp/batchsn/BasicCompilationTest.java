package cn.aiedge.erp.batchsn;

import cn.aiedge.erp.batchsn.entity.BatchNumber;
import cn.aiedge.erp.batchsn.ai.dto.PriceRecommendationDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基础编译测试 - 验证基本类型是否能够正确编译
 */
class BasicCompilationTest {
    
    @Test
    void testBatchNumberEntityCompilation() {
        // 测试BatchNumber实体类能否正确实例化
        BatchNumber batch = new BatchNumber();
        assertNotNull(batch, "BatchNumber实例不能为null");
        
        // 测试基本字段设置
        batch.setId(1L);
        batch.setBatchNo("TEST-BATCH-001");
        batch.setProductCode("PROD-001");
        batch.setProductName("测试产品");
        batch.setTotalQuantity(new BigDecimal("1000.00"));
        batch.setAvailableQuantity(new BigDecimal("800.00"));
        batch.setReservedQuantity(new BigDecimal("200.00"));
        batch.setProductionDate(LocalDate.now());
        batch.setExpirationDate(LocalDate.now().plusDays(365));
        batch.setBatchStatus("ACTIVE");
        batch.setSourceType("PRODUCTION");
        batch.setWarehouseName("中央仓库");
        batch.setCreatedBy("TEST_USER");
        batch.setCreatedAt(LocalDateTime.now());
        
        // 验证字段值
        assertEquals("TEST-BATCH-001", batch.getBatchNo());
        assertEquals("PROD-001", batch.getProductCode());
        assertEquals(new BigDecimal("1000.00"), batch.getTotalQuantity());
        assertEquals("ACTIVE", batch.getBatchStatus());
    }
    
    @Test
    void testPriceRecommendationDtoCompilation() {
        // 测试PriceRecommendationDto能否正确实例化
        PriceRecommendationDto dto = new PriceRecommendationDto();
        assertNotNull(dto, "PriceRecommendationDto实例不能为null");
        
        // 测试基本字段设置
        dto.setRequestId("REQ-001");
        dto.setProductId(123L);
        dto.setProductCode("PROD-001");
        dto.setProductName("测试产品");
        dto.setRecommendedPrice(99.99);
        dto.setConfidenceScore(0.85);
        dto.setCurrency("CNY");
        dto.setMinPrice(80.0);
        dto.setMaxPrice(120.0);
        dto.setAveragePrice(100.0);
        dto.setStrategyName("成本加成策略");
        
        // 验证字段值
        assertEquals("REQ-001", dto.getRequestId());
        assertEquals(123L, dto.getProductId());
        assertEquals(99.99, dto.getRecommendedPrice());
        assertEquals(0.85, dto.getConfidenceScore());
        assertEquals("CNY", dto.getCurrency());
        
        // 测试方法调用
        String recommendationLevel = dto.getRecommendationLevel();
        assertNotNull(recommendationLevel);
        
        boolean inRange = dto.isInReasonableRange();
        assertTrue(inRange, "推荐价格应在合理范围内");
        
        String formattedPrice = dto.getFormattedRecommendedPrice();
        assertNotNull(formattedPrice);
        
        String formattedRange = dto.getFormattedPriceRange();
        assertNotNull(formattedRange);
    }
    
    @Test
    void testPriceRecommendationDtoAlternativePrice() {
        // 测试嵌套类
        PriceRecommendationDto.AlternativePrice altPrice = 
            new PriceRecommendationDto.AlternativePrice("促销场景", 89.99, 0.75);
        
        assertEquals("促销场景", altPrice.getScenario());
        assertEquals(89.99, altPrice.getPrice());
        assertEquals(0.75, altPrice.getConfidence());
    }
    
    @Test
    void testPriceRecommendationDtoRiskAssessment() {
        // 测试嵌套类
        PriceRecommendationDto.RiskAssessment risk = 
            new PriceRecommendationDto.RiskAssessment(0.3, "low");
        
        assertEquals(0.3, risk.getRiskScore());
        assertEquals("low", risk.getRiskLevel());
    }
}