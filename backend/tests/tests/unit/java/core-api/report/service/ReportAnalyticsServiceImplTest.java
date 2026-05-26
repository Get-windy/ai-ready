package cn.aiedge.report.service.impl;

import cn.aiedge.report.model.ReportData;
import cn.aiedge.report.model.ReportDefinition;
import cn.aiedge.report.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 报表统计分析服务单元测试
 * 
 * @author AI-Ready QA Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportAnalyticsServiceImpl - 报表分析服务单元测试")
class ReportAnalyticsServiceImplTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportAnalyticsServiceImpl analyticsService;

    private final Long tenantId = 1L;
    private final String reportId = "sales_report";

    @BeforeEach
    void setUp() {
    }

    // ==================== 同比分析测试 ====================

    @Test
    @DisplayName("同比分析 - 成功场景")
    void testYearOverYearAnalysis_Success() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 31);
        
        ReportData currentData = createReportData(Arrays.asList(
            createRow("amount", 100000.0)
        ));
        ReportData prevData = createReportData(Arrays.asList(
            createRow("amount", 80000.0)
        ));
        
        when(reportService.generateReport(eq(reportId), anyMap(), eq(tenantId)))
            .thenReturn(currentData)
            .thenReturn(prevData);

        // When
        YoYResult result = analyticsService.yearOverYearAnalysis(reportId, "amount", startDate, endDate, tenantId);

        // Then
        assertNotNull(result);
        assertEquals(100000.0, result.getCurrentValue());
        assertEquals(80000.0, result.getPreviousValue());
        assertEquals(20000.0, result.getChangeAmount());
        assertEquals(25.0, result.getChangeRate());
    }

    @Test
    @DisplayName("同比分析 - 空数据场景")
    void testYearOverYearAnalysis_EmptyData() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 31);
        
        ReportData emptyData = createReportData(Collections.emptyList());
        
        when(reportService.generateReport(eq(reportId), anyMap(), eq(tenantId)))
            .thenReturn(emptyData)
            .thenReturn(emptyData);

        // When
        YoYResult result = analyticsService.yearOverYearAnalysis(reportId, "amount", startDate, endDate, tenantId);

        // Then
        assertNotNull(result);
        assertEquals(0.0, result.getCurrentValue());
        assertEquals(0.0, result.getPreviousValue());
    }

    @Test
    @DisplayName("同比分析 - 上期数据为0")
    void testYearOverYearAnalysis_ZeroPrevious() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 31);
        
        ReportData currentData = createReportData(Arrays.asList(
            createRow("amount", 100000.0)
        ));
        ReportData zeroData = createReportData(Arrays.asList(
            createRow("amount", 0.0)
        ));
        
        when(reportService.generateReport(eq(reportId), anyMap(), eq(tenantId)))
            .thenReturn(currentData)
            .thenReturn(zeroData);

        // When
        YoYResult result = analyticsService.yearOverYearAnalysis(reportId, "amount", startDate, endDate, tenantId);

        // Then
        assertNotNull(result);
        assertEquals(0.0, result.getChangeRate());
    }

    // ==================== 环比分析测试 ====================

    @Test
    @DisplayName("环比分析 - 月度环比")
    void testMonthOverMonthAnalysis_Monthly() {
        // Given
        LocalDate currentPeriod = LocalDate.of(2024, 3, 1);
        
        ReportData currentData = createReportData(Arrays.asList(
            createRow("sales", 150000.0)
        ));
        ReportData prevData = createReportData(Arrays.asList(
            createRow("sales", 120000.0)
        ));
        
        when(reportService.generateReport(eq(reportId), anyMap(), eq(tenantId)))
            .thenReturn(currentData)
            .thenReturn(prevData);

        // When
        MoMResult result = analyticsService.monthOverMonthAnalysis(reportId, "sales", "month", currentPeriod, tenantId);

        // Then
        assertNotNull(result);
        assertEquals(150000.0, result.getCurrentValue());
        assertEquals(120000.0, result.getPreviousValue());
        assertEquals(25.0, result.getChangeRate());
    }

    @Test
    @DisplayName("环比分析 - 季度环比")
    void testMonthOverMonthAnalysis_Quarterly() {
        // Given
        LocalDate currentPeriod = LocalDate.of(2024, 3, 1);
        
        ReportData currentData = createReportData(Arrays.asList(
            createRow("sales", 450000.0)
        ));
        ReportData prevData = createReportData(Arrays.asList(
            createRow("sales", 400000.0)
        ));
        
        when(reportService.generateReport(eq(reportId), anyMap(), eq(tenantId)))
            .thenReturn(currentData)
            .thenReturn(prevData);

        // When
        MoMResult result = analyticsService.monthOverMonthAnalysis(reportId, "sales", "quarter", currentPeriod, tenantId);

        // Then
        assertNotNull(result);
        assertTrue(result.getChangeRate() > 0);
    }

    // ==================== 趋势分析测试 ====================

    @Test
    @DisplayName("趋势分析 - 日趋势")
    void testTrendAnalysis_Daily() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 3, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 7);
        
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            rows.add(createRow("value", 1000.0 * i));
        }
        ReportData reportData = createReportData(rows);
        
        when(reportService.generateReport(eq(reportId), anyMap(), eq(tenantId)))
            .thenReturn(reportData);

        // When
        TrendResult result = analyticsService.trendAnalysis(reportId, "value", "day", startDate, endDate, tenantId);

        // Then
        assertNotNull(result);
        assertFalse(result.getDataPoints().isEmpty());
    }

    @Test
    @DisplayName("趋势分析 - 周趋势")
    void testTrendAnalysis_Weekly() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 31);
        
        ReportData reportData = createReportData(Arrays.asList(
            createRow("value", 50000.0),
            createRow("value", 60000.0),
            createRow("value", 55000.0)
        ));
        
        when(reportService.generateReport(eq(reportId), anyMap(), eq(tenantId)))
            .thenReturn(reportData);

        // When
        TrendResult result = analyticsService.trendAnalysis(reportId, "value", "week", startDate, endDate, tenantId);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("趋势分析 - 月趋势")
    void testTrendAnalysis_Monthly() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        
        ReportData reportData = createReportData(Arrays.asList(
            createRow("value", 100000.0),
            createRow("value", 120000.0),
            createRow("value", 110000.0)
        ));
        
        when(reportService.generateReport(eq(reportId), anyMap(), eq(tenantId)))
            .thenReturn(reportData);

        // When
        TrendResult result = analyticsService.trendAnalysis(reportId, "value", "month", startDate, endDate, tenantId);

        // Then
        assertNotNull(result);
    }

    // ==================== 辅助方法 ====================

    private ReportData createReportData(List<Map<String, Object>> rows) {
        ReportData data = new ReportData();
        data.setRows(rows);
        data.setTotal(rows.size());
        return data;
    }

    private Map<String, Object> createRow(String field, Double value) {
        Map<String, Object> row = new HashMap<>();
        row.put(field, value);
        return row;
    }

}
