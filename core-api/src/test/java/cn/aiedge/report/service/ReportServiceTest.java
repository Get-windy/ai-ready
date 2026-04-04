package cn.aiedge.report.service;

import cn.aiedge.report.model.ReportData;
import cn.aiedge.report.model.ReportDefinition;
import cn.aiedge.report.service.impl.ReportServiceImpl;
import cn.aiedge.cache.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 报表服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private ReportServiceImpl reportService;

    private Map<String, Object> defaultParameters;

    @BeforeEach
    void setUp() {
        defaultParameters = new HashMap<>();
        defaultParameters.put("startDate", "2024-01-01");
        defaultParameters.put("endDate", "2024-12-31");
    }

    @Test
    @DisplayName("获取报表定义 - 内置报表")
    void testGetReportDefinition_Builtin() {
        // 获取内置销售汇总报表
        ReportDefinition definition = reportService.getReportDefinition("sales_summary");
        
        assertNotNull(definition);
        assertEquals("sales_summary", definition.getReportId());
        assertEquals("销售汇总报表", definition.getReportName());
        assertEquals("summary", definition.getReportType());
        assertEquals("sales", definition.getCategory());
        assertNotNull(definition.getColumns());
        assertFalse(definition.getColumns().isEmpty());
    }

    @Test
    @DisplayName("获取报表定义 - 不存在的报表")
    void testGetReportDefinition_NotFound() {
        ReportDefinition definition = reportService.getReportDefinition("non_existent_report");
        assertNull(definition);
    }

    @Test
    @DisplayName("获取报表列表 - 全部")
    void testGetReportList_All() {
        List<ReportDefinition> reports = reportService.getReportList(null, 1L);
        
        assertNotNull(reports);
        assertFalse(reports.isEmpty());
        // 应包含4个内置报表
        assertTrue(reports.size() >= 4);
    }

    @Test
    @DisplayName("获取报表列表 - 按分类筛选")
    void testGetReportList_ByCategory() {
        List<ReportDefinition> reports = reportService.getReportList("sales", 1L);
        
        assertNotNull(reports);
        assertFalse(reports.isEmpty());
        
        // 所有报表都应该属于sales分类
        for (ReportDefinition report : reports) {
            assertEquals("sales", report.getCategory());
        }
    }

    @Test
    @DisplayName("生成销售汇总报表")
    void testGenerateReport_SalesSummary() {
        ReportData reportData = reportService.generateReport("sales_summary", defaultParameters, 1L);
        
        assertNotNull(reportData);
        assertEquals("sales_summary", reportData.getReportId());
        assertEquals("销售汇总报表", reportData.getReportName());
        assertNotNull(reportData.getGeneratedAt());
        assertNotNull(reportData.getRows());
        assertFalse(reportData.getRows().isEmpty());
        assertNotNull(reportData.getSummary());
        assertTrue(reportData.getQueryTime() >= 0);
    }

    @Test
    @DisplayName("生成客户分析报表 - 包含图表数据")
    void testGenerateReport_CustomerAnalysis() {
        ReportData reportData = reportService.generateReport("customer_analysis", defaultParameters, 1L);
        
        assertNotNull(reportData);
        assertEquals("customer_analysis", reportData.getReportId());
        assertNotNull(reportData.getRows());
        assertFalse(reportData.getRows().isEmpty());
        
        // 验证图表数据
        assertNotNull(reportData.getChartData());
        assertNotNull(reportData.getChartData().getLabels());
        assertNotNull(reportData.getChartData().getDatasets());
        assertFalse(reportData.getChartData().getLabels().isEmpty());
        assertFalse(reportData.getChartData().getDatasets().isEmpty());
    }

    @Test
    @DisplayName("生成产品销售报表")
    void testGenerateReport_ProductSales() {
        ReportData reportData = reportService.generateReport("product_sales", defaultParameters, 1L);
        
        assertNotNull(reportData);
        assertEquals("product_sales", reportData.getReportId());
        assertNotNull(reportData.getRows());
        assertFalse(reportData.getRows().isEmpty());
        
        // 验证汇总数据
        assertNotNull(reportData.getSummary());
        assertTrue(reportData.getSummary().containsKey("quantity"));
        assertTrue(reportData.getSummary().containsKey("amount"));
    }

    @Test
    @DisplayName("生成订单明细报表")
    void testGenerateReport_OrderDetail() {
        ReportData reportData = reportService.generateReport("order_detail", defaultParameters, 1L);
        
        assertNotNull(reportData);
        assertEquals("order_detail", reportData.getReportId());
        assertNotNull(reportData.getRows());
        assertFalse(reportData.getRows().isEmpty());
    }

    @Test
    @DisplayName("生成报表 - 无效报表ID")
    void testGenerateReport_InvalidReportId() {
        assertThrows(RuntimeException.class, () -> {
            reportService.generateReport("invalid_report", defaultParameters, 1L);
        });
    }

    @Test
    @DisplayName("预览报表")
    void testPreviewReport() {
        int maxRows = 5;
        ReportData reportData = reportService.previewReport("order_detail", defaultParameters, maxRows, 1L);
        
        assertNotNull(reportData);
        assertNotNull(reportData.getRows());
        assertTrue(reportData.getRows().size() <= maxRows);
    }

    @Test
    @DisplayName("导出Excel")
    void testExportToExcel() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> {
            reportService.exportToExcel("sales_summary", defaultParameters, outputStream, 1L);
        });
        
        byte[] excelBytes = outputStream.toByteArray();
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
    }

    @Test
    @DisplayName("导出CSV")
    void testExportToCsv() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> {
            reportService.exportToCsv("sales_summary", defaultParameters, outputStream, 1L);
        });
        
        byte[] csvBytes = outputStream.toByteArray();
        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 0);
        
        String csvContent = new String(csvBytes);
        assertTrue(csvContent.contains("日期")); // 包含表头
        assertTrue(csvContent.contains("合计")); // 包含汇总行
    }

    @Test
    @DisplayName("保存自定义报表定义")
    void testSaveReportDefinition() {
        ReportDefinition definition = new ReportDefinition();
        definition.setReportName("测试报表");
        definition.setReportCode("TEST_REPORT");
        definition.setReportType("detail");
        definition.setCategory("test");
        
        ReportDefinition saved = reportService.saveReportDefinition(definition, 1L);
        
        assertNotNull(saved);
        assertNotNull(saved.getReportId());
        assertEquals("测试报表", saved.getReportName());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        
        verify(cacheService, times(1)).set(anyString(), any());
    }

    @Test
    @DisplayName("复制报表定义")
    void testCopyReportDefinition() {
        String newName = "复制的销售报表";
        ReportDefinition copied = reportService.copyReportDefinition("sales_summary", newName, 1L);
        
        assertNotNull(copied);
        assertNotNull(copied.getReportId());
        assertEquals(newName, copied.getReportName());
        assertEquals("summary", copied.getReportType());
        assertEquals("sales", copied.getCategory());
        
        // 验证不是同一个报表
        assertNotEquals("sales_summary", copied.getReportId());
    }

    @Test
    @DisplayName("删除自定义报表定义")
    void testDeleteReportDefinition_Custom() {
        // 先创建一个自定义报表
        ReportDefinition definition = new ReportDefinition();
        definition.setReportName("待删除报表");
        definition.setReportCode("TO_DELETE");
        ReportDefinition saved = reportService.saveReportDefinition(definition, 1L);
        
        // 删除
        boolean result = reportService.deleteReportDefinition(saved.getReportId(), 1L);
        assertTrue(result);
        
        verify(cacheService, times(1)).delete(anyString());
    }

    @Test
    @DisplayName("删除内置报表定义 - 应该失败")
    void testDeleteReportDefinition_Builtin() {
        // 尝试删除内置报表
        boolean result = reportService.deleteReportDefinition("sales_summary", 1L);
        assertFalse(result); // 内置报表不能删除
    }

    @Test
    @DisplayName("复制不存在的报表 - 应该抛出异常")
    void testCopyReportDefinition_NotFound() {
        assertThrows(RuntimeException.class, () -> {
            reportService.copyReportDefinition("non_existent", "新名称", 1L);
        });
    }

    @Test
    @DisplayName("验证报表列定义")
    void testReportColumns() {
        ReportDefinition definition = reportService.getReportDefinition("sales_summary");
        
        assertNotNull(definition.getColumns());
        assertFalse(definition.getColumns().isEmpty());
        
        // 验证第一个列定义
        ReportDefinition.ReportColumn column = definition.getColumns().get(0);
        assertNotNull(column.getField());
        assertNotNull(column.getTitle());
        assertNotNull(column.getDataType());
        assertTrue(column.getWidth() > 0);
    }

    @Test
    @DisplayName("验证报表参数定义")
    void testReportParameters() {
        ReportDefinition definition = reportService.getReportDefinition("sales_summary");
        
        assertNotNull(definition.getParameters());
        assertFalse(definition.getParameters().isEmpty());
        
        // 验证参数
        ReportDefinition.ReportParameter param = definition.getParameters().get(0);
        assertNotNull(param.getName());
        assertNotNull(param.getTitle());
        assertNotNull(param.getDataType());
    }

    @Test
    @DisplayName("验证图表配置")
    void testChartConfig() {
        ReportDefinition definition = reportService.getReportDefinition("customer_analysis");
        
        assertNotNull(definition.getChartConfig());
        assertEquals("bar", definition.getChartConfig().getChartType());
        assertNotNull(definition.getChartConfig().getxField());
        assertNotNull(definition.getChartConfig().getyFields());
    }

    @Test
    @DisplayName("测试汇总计算")
    void testSummaryCalculation() {
        ReportData reportData = reportService.generateReport("sales_summary", defaultParameters, 1L);
        
        Map<String, Object> summary = reportData.getSummary();
        assertNotNull(summary);
        
        // 验证汇总数据包含正确字段
        assertTrue(summary.containsKey("totalOrders") || summary.containsKey("totalAmount"));
    }

    @Test
    @DisplayName("测试空参数处理")
    void testNullParameters() {
        // 传入null参数
        ReportData reportData = reportService.generateReport("sales_summary", null, 1L);
        
        assertNotNull(reportData);
        assertNotNull(reportData.getRows());
    }
}
