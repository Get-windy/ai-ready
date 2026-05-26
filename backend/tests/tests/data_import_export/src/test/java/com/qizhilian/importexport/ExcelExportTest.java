package com.qizhilian.importexport;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Excel导出测试类
 * TC-EE-001 至 TC-EE-008
 */
@DisplayName("Excel导出测试")
public class ExcelExportTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    @DisplayName("TC-EE-001: 列表数据导出测试")
    @Tag("export")
    void testListDataExport() throws IOException {
        List<Map<String, Object>> data = generateTestData(100);
        
        ExportConfig config = ExportConfig.builder()
            .fileName("list_export.xlsx")
            .sheetName("数据列表")
            .columns(Arrays.asList("id", "name", "quantity", "price", "date"))
            .build();
        
        ExportResult result = exportToExcel(data, config);
        
        assertEquals("success", result.getStatus(), "列表导出应该成功");
        assertTrue(result.getFileSize() > 0, "导出文件大小应大于0");
        assertEquals(100, result.getRecordCount(), "应导出100条记录");
        
        System.out.println("列表导出成功，文件大小: " + result.getFileSize() + " bytes");
    }
    
    @Test
    @DisplayName("TC-EE-002: 带筛选条件导出测试")
    @Tag("export")
    void testFilteredExport() throws IOException {
        List<Map<String, Object>> data = generateTestData(1000);
        
        // 筛选条件：价格大于100
        ExportFilter filter = new ExportFilter();
        filter.addCondition("price", ">", 100);
        
        ExportConfig config = ExportConfig.builder()
            .fileName("filtered_export.xlsx")
            .filter(filter)
            .columns(Arrays.asList("id", "name", "price"))
            .build();
        
        ExportResult result = exportToExcel(data, config);
        
        assertEquals("success", result.getStatus(), "筛选导出应该成功");
        assertTrue(result.getRecordCount() < 1000, "筛选后记录数应少于原始数据");
        
        System.out.println("筛选导出成功，导出: " + result.getRecordCount() + "条记录");
    }
    
    @Test
    @DisplayName("TC-EE-003: 大数据量导出分页测试")
    @Tag("export")
    @Tag("performance")
    void testLargeDataExportWithPagination() throws IOException {
        List<Map<String, Object>> data = generateTestData(50000);
        
        ExportConfig config = ExportConfig.builder()
            .fileName("large_export.xlsx")
            .pageSize(1000)
            .enablePagination(true)
            .columns(Arrays.asList("id", "name", "quantity", "price"))
            .build();
        
        long startTime = System.currentTimeMillis();
        ExportResult result = exportToExcel(data, config);
        long duration = System.currentTimeMillis() - startTime;
        
        assertEquals("success", result.getStatus(), "大数据量导出应该成功");
        assertEquals(50000, result.getRecordCount(), "应导出50000条记录");
        assertTrue(duration < 60000, "导出50000条记录应在60秒内完成");
        
        System.out.println("大数据量导出成功，耗时: " + duration + "ms");
    }
    
    @Test
    @DisplayName("TC-EE-004: 导出字段自定义测试")
    @Tag("export")
    void testCustomFieldExport() throws IOException {
        List<Map<String, Object>> data = generateTestData(50);
        
        // 只导出部分字段
        List<ExportColumn> columns = Arrays.asList(
            new ExportColumn("id", "编号", 10),
            new ExportColumn("name", "产品名称", 30),
            new ExportColumn("price", "单价", 15)
        );
        
        ExportConfig config = ExportConfig.builder()
            .fileName("custom_fields.xlsx")
            .customColumns(columns)
            .build();
        
        ExportResult result = exportToExcel(data, config);
        
        assertEquals("success", result.getStatus(), "自定义字段导出应该成功");
        assertEquals(3, result.getExportedColumnCount(), "应导出3个字段");
        
        System.out.println("自定义字段导出成功，导出字段数: " + result.getExportedColumnCount());
    }
    
    @Test
    @DisplayName("TC-EE-005: Excel格式导出测试(.xlsx)")
    @Tag("export")
    void testXlsxFormatExport() throws IOException {
        List<Map<String, Object>> data = generateTestData(100);
        
        ExportConfig config = ExportConfig.builder()
            .fileName("xlsx_export.xlsx")
            .format("xlsx")
            .build();
        
        ExportResult result = exportToExcel(data, config);
        
        assertEquals("success", result.getStatus(), "XLSX导出应该成功");
        assertTrue(result.getFilePath().endsWith(".xlsx"), "文件扩展名应为.xlsx");
        
        System.out.println("XLSX格式导出成功");
    }
    
    @Test
    @DisplayName("TC-EE-006: CSV格式导出测试")
    @Tag("export")
    void testCsvFormatExport() throws IOException {
        List<Map<String, Object>> data = generateTestData(100);
        
        ExportConfig config = ExportConfig.builder()
            .fileName("csv_export.csv")
            .format("csv")
            .encoding("UTF-8")
            .build();
        
        ExportResult result = exportToExcel(data, config);
        
        assertEquals("success", result.getStatus(), "CSV导出应该成功");
        assertTrue(result.getFilePath().endsWith(".csv"), "文件扩展名应为.csv");
        assertTrue(result.getFileSize() < 50000, "CSV文件应比Excel更小");
        
        System.out.println("CSV格式导出成功，文件大小: " + result.getFileSize() + " bytes");
    }
    
    @Test
    @DisplayName("TC-EE-007: 异步导出测试")
    @Tag("export")
    void testAsyncExport() throws IOException, InterruptedException {
        List<Map<String, Object>> data = generateTestData(10000);
        
        ExportConfig config = ExportConfig.builder()
            .fileName("async_export.xlsx")
            .async(true)
            .callbackUrl("/api/export/callback")
            .build();
        
        ExportResult result = exportToExcelAsync(data, config);
        
        assertEquals("pending", result.getStatus(), "异步导出应返回pending状态");
        assertNotNull(result.getTaskId(), "应返回任务ID");
        
        // 模拟等待异步任务完成
        Thread.sleep(2000);
        ExportResult finalResult = checkExportStatus(result.getTaskId());
        
        assertEquals("success", finalResult.getStatus(), "异步导出最终应成功");
        
        System.out.println("异步导出成功，任务ID: " + result.getTaskId());
    }
    
    @Test
    @DisplayName("TC-EE-008: 导出模板下载测试")
    @Tag("export")
    void testExportTemplateDownload() throws IOException {
        ExportConfig config = ExportConfig.builder()
            .templateType("product")
            .columns(Arrays.asList("id", "name", "quantity", "price", "category"))
            .build();
        
        ExportResult result = downloadTemplate(config);
        
        assertEquals("success", result.getStatus(), "模板下载应该成功");
        assertTrue(result.getFilePath().contains("template"), "文件名应包含template");
        
        System.out.println("模板下载成功: " + result.getFilePath());
    }
    
    // Helper methods
    private List<Map<String, Object>> generateTestData(int count) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", "商品" + i);
            row.put("quantity", 100 + i);
            row.put("price", 50 + (i % 200));
            row.put("date", "2024-01-" + String.format("%02d", (i % 30) + 1));
            row.put("category", "类别" + (i % 5));
            data.add(row);
        }
        return data;
    }
    
    private ExportResult exportToExcel(List<Map<String, Object>> data, ExportConfig config) {
        // Mock implementation
