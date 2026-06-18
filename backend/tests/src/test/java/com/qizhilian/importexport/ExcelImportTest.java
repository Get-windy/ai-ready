package com.qizhilian.importexport;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Excel导入测试类
 * TC-IE-001 至 TC-IE-010
 */
@DisplayName("Excel导入测试")
public class ExcelImportTest {
    
    @TempDir
    Path tempDir;
    
    private File createTestExcel(String fileName, int sheetCount, int rowCount) throws IOException {
        File file = tempDir.resolve(fileName).toFile();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            for (int s = 0; s < sheetCount; s++) {
                Sheet sheet = workbook.createSheet("Sheet" + (s + 1));
                
                // 创建表头
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("ID");
                headerRow.createCell(1).setCellValue("名称");
                headerRow.createCell(2).setCellValue("数量");
                headerRow.createCell(3).setCellValue("价格");
                headerRow.createCell(4).setCellValue("日期");
                
                // 创建数据行
                for (int i = 1; i <= rowCount; i++) {
                    Row row = sheet.createRow(i);
                    row.createCell(0).setCellValue(i);
                    row.createCell(1).setCellValue("商品" + i);
                    row.createCell(2).setCellValue(100 + i);
                    row.createCell(3).setCellValue(99.99 + i);
                    row.createCell(4).setCellValue("2024-01-" + String.format("%02d", i));
                }
            }
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
        
        return file;
    }
    
    @Test
    @DisplayName("TC-IE-001: 单表数据导入测试")
    @Tag("import")
    void testSingleSheetImport() throws IOException {
        File excelFile = createTestExcel("single_sheet.xlsx", 1, 10);
        
        ImportResult result = importExcel(excelFile, ImportConfig.singleSheet());
        
        assertEquals("success", result.getStatus(), "单表导入应该成功");
        assertEquals(10, result.getImportedCount(), "应导入10条记录");
        assertEquals(0, result.getErrorCount(), "错误数应为0");
        
        System.out.println("单表导入成功，导入: " + result.getImportedCount() + "条记录");
    }
    
    @Test
    @DisplayName("TC-IE-002: 多Sheet导入测试")
    @Tag("import")
    void testMultiSheetImport() throws IOException {
        File excelFile = createTestExcel("multi_sheet.xlsx", 3, 5);
        
        ImportResult result = importExcel(excelFile, ImportConfig.multiSheet());
        
        assertEquals("success", result.getStatus(), "多Sheet导入应该成功");
        assertEquals(3, result.getSheetCount(), "应导入3个Sheet");
        assertEquals(15, result.getImportedCount(), "应导入15条记录");
        
        System.out.println("多Sheet导入成功，共导入: " + result.getImportedCount() + "条记录");
    }
    
    @Test
    @DisplayName("TC-IE-003: 大数据量导入性能测试")
    @Tag("import")
    @Tag("performance")
    void testLargeDataImport() throws IOException {
        File excelFile = createTestExcel("large_data.xlsx", 1, 10000);
        
        long startTime = System.currentTimeMillis();
        ImportResult result = importExcel(excelFile, ImportConfig.largeData());
        long duration = System.currentTimeMillis() - startTime;
        
        assertEquals("success", result.getStatus(), "大数据量导入应该成功");
        assertEquals(10000, result.getImportedCount(), "应导入10000条记录");
        assertTrue(duration < 30000, "导入10000条记录应在30秒内完成，实际耗时: " + duration + "ms");
        
        System.out.println("大数据量导入成功，耗时: " + duration + "ms，速度: " + (10000.0 / duration * 1000) + "条/秒");
    }
    
    @Test
    @DisplayName("TC-IE-004: 必填项验证测试")
    @Tag("import")
    @Tag("validation")
    void testRequiredFieldValidation() throws IOException {
        File file = tempDir.resolve("missing_required.xlsx").toFile();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("名称");
            
            // 缺少必填字段的行
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue(1);
            row1.createCell(1).setCellValue("商品1");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue(2);
            // 名称留空
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
        
        ImportResult result = importExcel(file, ImportConfig.withValidation());
        
        assertEquals("partial", result.getStatus(), "应有部分失败");
        assertTrue(result.getErrorCount() > 0, "应有验证错误");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("必填")), "错误信息应包含必填提示");
        
        System.out.println("必填项验证测试完成，错误数: " + result.getErrorCount());
    }
    
    @Test
    @DisplayName("TC-IE-005: 数据格式验证测试")
    @Tag("import")
    @Tag("validation")
    void testDataFormatValidation() throws IOException {
        File file = tempDir.resolve("invalid_format.xlsx").toFile();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("邮箱");
            headerRow.createCell(2).setCellValue("日期");
            
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue(1);
            row1.createCell(1).setCellValue("valid@example.com");
            row1.createCell(2).setCellValue("2024-01-01");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue(2);
            row2.createCell(1).setCellValue("invalid-email"); // 无效邮箱格式
            row2.createCell(2).setCellValue("2024-13-45"); // 无效日期
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
        
        ImportResult result = importExcel(file, ImportConfig.withFormatValidation());
        
        assertEquals("partial", result.getStatus(), "应有部分失败");
        assertTrue(result.getErrorCount() > 0, "应有格式验证错误");
        
        System.out.println("数据格式验证测试完成，错误数: " + result.getErrorCount());
    }
    
    @Test
    @DisplayName("TC-IE-006: 重复数据检测测试")
    @Tag("import")
    @Tag("validation")
    void testDuplicateDataDetection() throws IOException {
        File file = tempDir.resolve("duplicate_data.xlsx").toFile();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("名称");
            
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("PROD-001");
            row1.createCell(1).setCellValue("商品A");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("PROD-001"); // 重复的ID
            row2.createCell(1).setCellValue("商品B");
            
