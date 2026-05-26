package cn.aiedge.erp.metrics.service;

import cn.aiedge.erp.metrics.dto.*;
import cn.aiedge.erp.metrics.entity.BusinessMetric;
import cn.aiedge.erp.metrics.enums.MetricPeriod;
import cn.aiedge.erp.metrics.enums.MetricStatus;
import cn.aiedge.erp.metrics.enums.MetricType;
import cn.aiedge.erp.metrics.repository.BusinessMetricRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 业务指标服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock
    private BusinessMetricRepository metricRepository;
    
    @Mock
    private MetricsCalculationService calculationService;
    
    @InjectMocks
    private MetricsServiceImpl metricsService;
    
    private BusinessMetric testMetric;
    
    @BeforeEach
    void setUp() {
        testMetric = BusinessMetric.builder()
                .id(1L)
                .metricCode("order_total_today")
                .metricName("今日订单总数")
                .metricType(MetricType.ORDER)
                .period(MetricPeriod.REALTIME)
                .unit("单")
                .status(MetricStatus.ACTIVE)
                .build();
    }
    
    @Test
    void testGetDashboardMetrics() {
        // Given
        when(metricRepository.findByMetricCode(anyString())).thenReturn(Optional.of(testMetric));
        
        // When
        DashboardMetricsDTO result = metricsService.getDashboardMetrics();
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getUpdateTime());
        assertFalse(result.getCards().isEmpty());
        assertFalse(result.getCharts().isEmpty());
    }
    
    @Test
    void testGetMetricsByType() {
        // Given
        when(metricRepository.findActiveMetricsByType(MetricType.ORDER))
                .thenReturn(Arrays.asList(testMetric));
        
        // When
        List<MetricValueDTO> result = metricsService.getMetricsByType(MetricType.ORDER);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("order_total_today", result.get(0).getMetricCode());
    }
    
    @Test
    void testGetCurrentMetric() {
        // Given
        when(metricRepository.findByMetricCode("order_total_today"))
                .thenReturn(Optional.of(testMetric));
        when(calculationService.calculateMetric(any(), any()))
                .thenReturn(new BigDecimal(156));
        
        // When
        MetricValueDTO result = metricsService.getCurrentMetric("order_total_today");
        
        // Then
        assertNotNull(result);
        assertEquals("order_total_today", result.getMetricCode());
        assertNotNull(result.getValue());
    }
    
    @Test
    void testGetCurrentMetric_NotFound() {
        // Given
        when(metricRepository.findByMetricCode("unknown_metric"))
                .thenReturn(Optional.empty());
        
        // Then
        assertThrows(IllegalArgumentException.class, () -> {
            metricsService.getCurrentMetric("unknown_metric");
        });
    }
    
    @Test
    void testGetMetricHistory() {
        // Given
        MetricQueryRequest request = MetricQueryRequest.builder()
                .metricCode("order_total_today")
                .startTime(LocalDateTime.now().minusDays(7))
                .endTime(LocalDateTime.now())
                .period("1h")
                .build();
        
        when(metricRepository.findByMetricCode("order_total_today"))
                .thenReturn(Optional.of(testMetric));
        
        // When
        MetricHistoryDTO result = metricsService.getMetricHistory(request);
        
        // Then
        assertNotNull(result);
        assertEquals("order_total_today", result.getMetricCode());
        assertNotNull(result.getDataPoints());
    }
    
    @Test
    void testGetAllActiveMetrics() {
        // Given
        when(metricRepository.findAllActiveMetrics())
                .thenReturn(Arrays.asList(testMetric));
        
        // When
        List<BusinessMetric> result = metricsService.getAllActiveMetrics();
        
        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }
    
    @Test
    void testRefreshMetrics() {
        // Given
        when(metricRepository.findActiveMetricsByType(any()))
                .thenReturn(Arrays.asList(testMetric));
        
        // When & Then (should not throw exception)
        assertDoesNotThrow(() -> metricsService.refreshMetrics());
    }
}

// 简单的实现类用于测试
class MetricsServiceImpl implements MetricsService {
    
    private final BusinessMetricRepository metricRepository;
    private final MetricsCalculationService calculationService;
    
    public MetricsServiceImpl(BusinessMetricRepository metricRepository, 
                              MetricsCalculationService calculationService) {
        this.metricRepository = metricRepository;
        this.calculationService = calculationService;
    }
    
    @Override
    public DashboardMetricsDTO getDashboardMetrics() {
        DashboardMetricsDTO dto = new DashboardMetricsDTO();
        dto.setUpdateTime(LocalDateTime.now());
        // 简化实现
        return dto;
    }
    
    @Override
    public List<MetricValueDTO> getMetricsByType(MetricType type) {
        return List.of();
    }
    
    @Override
    public MetricHistoryDTO getMetricHistory(MetricQueryRequest request) {
        BusinessMetric metric = metricRepository.findByMetricCode(request.getMetricCode())
                .orElseThrow(() -> new IllegalArgumentException("Metric not found"));
        
        MetricHistoryDTO dto = new MetricHistoryDTO();
        dto.setMetricCode(metric.getMetricCode());
        dto.setMetricName(metric.getMetricName());
        dto.setUnit(metric.getUnit());
        dto.setPeriod(request.getPeriod());
        dto.setDataPoints(List.of());
        return dto;
    }
    
    @Override
    public MetricValueDTO getCurrentMetric(String metricCode) {
        BusinessMetric metric = metricRepository.findByMetricCode(metricCode)
                .orElseThrow(() -> new IllegalArgumentException("Metric not found"));
        
        MetricValueDTO dto = new MetricValueDTO();
        dto.setMetricCode(metric.getMetricCode());
        dto.setMetricName(metric.getMetricName());
        dto.setUnit(metric.getUnit());
        dto.setValue(calculationService.calculateMetric(metric, LocalDateTime.now()));
        dto.setTimestamp(LocalDateTime.now());
        return dto;
    }
    
    @Override
    public List<MetricValueDTO> getCurrentMetrics(List<String> metricCodes) {
        return List.of();
    }
    
    @Override
    public List<BusinessMetric> getAllActiveMetrics() {
        return metricRepository.findAllActiveMetrics();
    }
    
    @Override
    public void refreshMetrics() {
        // 简化实现
    }
}