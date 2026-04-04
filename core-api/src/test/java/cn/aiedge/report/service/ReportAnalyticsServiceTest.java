package cn.aiedge.report.service;

import cn.aiedge.report.model.ReportData;
import cn.aiedge.report.model.ReportDefinition;
import cn.aiedge.report.service.impl.ReportAnalyticsServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 报表统计分析服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ReportAnalyticsServiceTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportAnalyticsServiceImpl analyticsService;

    private ReportData testReportData;

    @BeforeEach
    void setUp() {
        // 创建测试报表数据
        testReportData = new ReportData();
        testReportData.setReportId("test_report");
        testReportData.setReportName("测试报表");
        testReportData.setGeneratedAt(java.time.LocalDateTime.now());

        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("amount", 1000 * i);
            row.put("quantity", 10 * i);
            row.put("customerName", "客户" + i);
            row.put("date", LocalDate.now().minusDays(i).toString());
            rows.add(row);
        }
        testReportData.setRows(rows);
        testReportData.setTotalRows(rows.size());

        // 模拟报表服务
        when(reportService.generateReport(anyString(), any(), anyLong()))
            .thenReturn(testReportData);
        
        ReportDefinition definition = new ReportDefinition();
        definition.setReportId("test_report");
        definition.setReportName("测试报表");
        when(reportService.getReportDefinition(anyString())).thenReturn(definition);
    }

    @Test
    @DisplayName("同比分析 - 正常计算")
    void testYearOverYearAnalysis() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        ReportAnalyticsService.YoYResult result = analyticsService
            .yearOverYearAnalysis("test_report", "amount", startDate, endDate, 1L);

        assertNotNull(result);
        assertEquals("amount", result.field());
        assertNotNull(result.period());
        assertNotNull(result.previousPeriod());
        assertTrue(result.currentValue() > 0);
    }

    @Test
    @DisplayName("环比分析 - 月度")
    void testMonthOverMonthAnalysis_Monthly() {
        LocalDate currentPeriod = LocalDate.of(2024, 6, 15);

        ReportAnalyticsService.MoMResult result = analyticsService
            .monthOverMonthAnalysis("test_report", "amount", "month", currentPeriod, 1L);

        assertNotNull(result);
        assertEquals("amount", result.field());
        assertEquals("month", result.periodType());
        assertNotNull(result.currentPeriod());
        assertNotNull(result.previousPeriod());
        assertTrue(result.currentValue() > 0);
    }

    @Test
    @DisplayName("环比分析 - 周度")
    void testMonthOverMonthAnalysis_Weekly() {
        LocalDate currentPeriod = LocalDate.of(2024, 6, 15);

        ReportAnalyticsService.MoMResult result = analyticsService
            .monthOverMonthAnalysis("test_report", "amount", "week", currentPeriod, 1L);

        assertNotNull(result);
        assertEquals("week", result.periodType());
    }

    @Test
    @DisplayName("趋势分析 - 日粒度")
    void testTrendAnalysis_DayGranularity() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        ReportAnalyticsService.TrendResult result = analyticsService
            .trendAnalysis("test_report", "amount", startDate, endDate, "day", 1L);

        assertNotNull(result);
        assertEquals("amount", result.field());
        assertEquals("day", result.granularity());
        assertNotNull(result.dataPoints());
        assertFalse(result.dataPoints().isEmpty());
        assertNotNull(result.trend());
        assertTrue(result.r2() >= 0 && result.r2() <= 1);
    }

    @Test
    @DisplayName("趋势分析 - 周粒度")
    void testTrendAnalysis_WeekGranularity() {
        LocalDate startDate = LocalDate.now().minusWeeks(4);
        LocalDate endDate = LocalDate.now();

        ReportAnalyticsService.TrendResult result = analyticsService
            .trendAnalysis("test_report", "quantity", startDate, endDate, "week", 1L);

        assertNotNull(result);
        assertEquals("week", result.granularity());
    }

    @Test
    @DisplayName("排名分析 - 降序")
    void testRankingAnalysis_Descending() {
        ReportAnalyticsService.RankingResult result = analyticsService
            .rankingAnalysis("test_report", "amount", "customerName", 5, false, 1L);

        assertNotNull(result);
        assertEquals("amount", result.rankField());
        assertEquals("customerName", result.groupField());
        assertNotNull(result.rankings());
        assertTrue(result.rankings().size() <= 5);
        assertTrue(result.totalItems() > 0);

        // 验证排名是否按降序排列
        for (int i = 1; i < result.rankings().size(); i++) {
            assertTrue(result.rankings().get(i - 1).value() >= result.rankings().get(i).value());
        }
    }

    @Test
    @DisplayName("排名分析 - 升序")
    void testRankingAnalysis_Ascending() {
        ReportAnalyticsService.RankingResult result = analyticsService
            .rankingAnalysis("test_report", "amount", "customerName", 5, true, 1L);

        assertNotNull(result);
        
        // 验证排名是否按升序排列
        for (int i = 1; i < result.rankings().size(); i++) {
            assertTrue(result.rankings().get(i - 1).value() <= result.rankings().get(i).value());
        }
    }

    @Test
    @DisplayName("占比分析")
    void testProportionAnalysis() {
        ReportAnalyticsService.ProportionResult result = analyticsService
            .proportionAnalysis("test_report", "amount", "customerName", 1L);

        assertNotNull(result);
        assertEquals("amount", result.valueField());
        assertEquals("customerName", result.groupField());
        assertTrue(result.total() > 0);
        assertNotNull(result.items());
        assertFalse(result.items().isEmpty());

        // 验证百分比总和约为100%
        double totalPercentage = result.items().stream()
            .mapToDouble(ReportAnalyticsService.ProportionResult.ProportionItem::percentage)
            .sum();
        assertEquals(100.0, totalPercentage, 0.01);
    }

    @Test
    @DisplayName("分布分析")
    void testDistributionAnalysis() {
        ReportAnalyticsService.DistributionResult result = analyticsService
            .distributionAnalysis("test_report", "amount", 5, 1L);

        assertNotNull(result);
        assertEquals("amount", result.field());
        assertNotNull(result.buckets());
        assertEquals(5, result.buckets().size());
        assertTrue(result.min() <= result.max());
        assertTrue(result.mean() > 0);
        assertTrue(result.stdDev() >= 0);

        // 验证分桶百分比总和为100%
        double totalPercentage = result.buckets().stream()
            .mapToDouble(ReportAnalyticsService.DistributionResult.Bucket::percentage)
            .sum();
        assertEquals(100.0, totalPercentage, 0.01);
    }

    @Test
    @DisplayName("异常检测 - 中等敏感度")
    void testAnomalyDetection_MediumSensitivity() {
        // 添加一个异常值
        Map<String, Object> anomalyRow = new HashMap<>();
        anomalyRow.put("amount", 1000000); // 明显偏离的值
        testReportData.getRows().add(anomalyRow);

        ReportAnalyticsService.AnomalyResult result = analyticsService
            .anomalyDetection("test_report", "amount", "medium", 1L);

        assertNotNull(result);
        assertEquals("amount", result.field());
        assertTrue(result.mean() > 0);
        assertTrue(result.stdDev() >= 0);
        assertNotNull(result.anomalies());
        assertTrue(result.totalChecked() > 0);
    }

    @Test
    @DisplayName("异常检测 - 高敏感度")
    void testAnomalyDetection_HighSensitivity() {
        ReportAnalyticsService.AnomalyResult result = analyticsService
            .anomalyDetection("test_report", "amount", "high", 1L);

        assertNotNull(result);
        // 高敏感度应该检测出更多异常
        assertTrue(result.lowerThreshold() < result.upperThreshold());
    }

    @Test
    @DisplayName("综合统计报告")
    void testGenerateComprehensiveReport() {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        List<String> fields = Arrays.asList("amount", "quantity");

        ReportAnalyticsService.ComprehensiveReport report = analyticsService
            .generateComprehensiveReport("test_report", fields, startDate, endDate, 1L);

        assertNotNull(report);
        assertEquals("test_report", report.reportId());
        assertEquals(startDate, report.startDate());
        assertEquals(endDate, report.endDate());
        assertNotNull(report.summary());
        assertNotNull(report.yoyResults());
        assertNotNull(report.momResults());
        assertNotNull(report.trends());
        assertNotNull(report.recommendations());
        assertEquals(2, report.yoyResults().size());
        assertEquals(2, report.momResults().size());
        assertEquals(2, report.trends().size());
    }

    @Test
    @DisplayName("综合统计报告 - 空字段列表")
    void testGenerateComprehensiveReport_EmptyFields() {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        List<String> fields = Collections.emptyList();

        ReportAnalyticsService.ComprehensiveReport report = analyticsService
            .generateComprehensiveReport("test_report", fields, startDate, endDate, 1L);

        assertNotNull(report);
        assertTrue(report.yoyResults().isEmpty());
        assertTrue(report.momResults().isEmpty());
        assertTrue(report.trends().isEmpty());
    }

    @Test
    @DisplayName("验证同比变化率计算")
    void testYoYChangeRateCalculation() {
        // 创建特定数据测试变化率计算
        ReportData currentData = new ReportData();
        currentData.setRows(List.of(Map.of("amount", 1200.0)));
        
        ReportData previousData = new ReportData();
        previousData.setRows(List.of(Map.of("amount", 1000.0)));

        when(reportService.generateReport(anyString(), any(), anyLong()))
            .thenReturn(currentData)
            .thenReturn(previousData);

        ReportAnalyticsService.YoYResult result = analyticsService
            .yearOverYearAnalysis("test_report", "amount", 
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), 1L);

        assertNotNull(result);
        assertEquals(200.0, result.changeAmount(), 0.01);
        assertEquals(20.0, result.changeRate(), 0.01);
    }

    @Test
    @DisplayName("验证排名百分比计算")
    void testRankingPercentageCalculation() {
        ReportAnalyticsService.RankingResult result = analyticsService
            .rankingAnalysis("test_report", "amount", "customerName", 10, false, 1L);

        // 验证所有排名项的百分比都在0-100之间
        for (ReportAnalyticsService.RankingResult.RankItem item : result.rankings()) {
            assertTrue(item.percentage() >= 0 && item.percentage() <= 100);
        }
    }

    @Test
    @DisplayName("验证趋势预测")
    void testTrendForecast() {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        ReportAnalyticsService.TrendResult result = analyticsService
            .trendAnalysis("test_report", "amount", startDate, endDate, "day", 1L);

        assertNotNull(result.forecast());
        assertFalse(result.forecast().isEmpty());
    }

    @Test
    @DisplayName("空数据处理")
    void testEmptyDataHandling() {
        // 创建空数据报表
        ReportData emptyData = new ReportData();
        emptyData.setRows(Collections.emptyList());
        
        when(reportService.generateReport(anyString(), any(), anyLong()))
            .thenReturn(emptyData);

        ReportAnalyticsService.DistributionResult result = analyticsService
            .distributionAnalysis("test_report", "amount", 5, 1L);

        assertNotNull(result);
        assertEquals(0, result.min());
        assertEquals(0, result.max());
        assertTrue(result.buckets().isEmpty());
    }
}
