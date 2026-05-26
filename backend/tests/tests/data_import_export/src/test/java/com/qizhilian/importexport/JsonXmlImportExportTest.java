package com.qizhilian.importexport;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * JSON/XML导入导出测试类
 * TC-JX-001 至 TC-JX-006
 */
@DisplayName("JSON/XML导入导出测试")
public class JsonXmlImportExportTest {
    
    @TempDir
    Path tempDir;
    
    private ObjectMapper mapper;
    
    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    @Test
    @DisplayName("TC-JX-001: API数据JSON导入测试")
    @Tag("json")
    @Tag("import")
    void testApiJsonImport() throws IOException {
        // 创建测试JSON数据
        Map<String, Object> apiData = new HashMap<>();
        apiData.put("source", "external_api");
        apiData.put("timestamp", System.currentTimeMillis());
        
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", "API-" + i);
            record.put("name", "API商品" + i);
            record.put("price", 100.0 * i);
            records.add(record);
        }
        apiData.put("data", records);
        
        File jsonFile = tempDir.resolve("api_import.json").toFile();
        mapper.writeValue(jsonFile, apiData);
        
        ImportResult result = importJsonData(jsonFile, ImportConfig.apiImport());
        
        assertEquals("success", result.getStatus(), "API JSON导入应该成功");
        assertEquals(5, result.getImportedCount(), "应导入5条记录");
        
        System.out.println("API JSON导入成功，导入: " + result.getImportedCount() + "条记录");
    }
    
    @Test
    @DisplayName("TC-JX-002: 系统配置JSON导出测试")
    @Tag("json")
    @Tag("export")
    void testSystemConfigJsonExport() throws IOException {
        // 模拟系统配置数据
        Map<String, Object> config = new HashMap<>();
        config.put("systemName", "企智连");
        config.put("version", "2.0.0");
        config.put("database", Map.of(
            "host", "localhost",
            "port", 3306,
            "name", "qizhilian"
        ));
        config.put("features", Arrays.asList("inventory", "sales", "finance"));
        
        ExportConfig exportConfig = ExportConfig.builder()
            .fileName("system_config.json")
            .format("json")
            .build();
        
        ExportResult result = exportToJson(config, exportConfig);
        
        assertEquals("success", result.getStatus(), "系统配置导出应该成功");
        assertTrue(result.getFileSize() > 0, "导出文件大小应大于0");
        
        System.out.println("系统配置JSON导出成功");
    }
    
    @Test
    @DisplayName("TC-JX-003: JSON数据格式验证测试")
    @Tag("json")
    @Tag("validation")
    void testJsonFormatValidation() throws IOException {
        // 创建格式错误的JSON
        File invalidJson = tempDir.resolve("invalid.json").toFile();
        try (FileWriter writer = new FileWriter(invalidJson)) {
            writer.write("{\"id\": 1, \"name\": \"test\", }"); // 多余的逗号
        }
        
        ImportResult result = importJsonData(invalidJson, ImportConfig.withValidation());
        
        assertEquals("failed", result.getStatus(), "格式错误的JSON应该失败");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("格式") || e.contains("parse")),
            "错误信息应包含格式相关提示");
        
        System.out.println("JSON格式验证测试完成");
    }
    
    @Test
    @DisplayName("TC-JX-004: XML数据导出测试")
    @Tag("xml")
    @Tag("export")
    void testXmlExport() throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", i);
            record.put("name", "产品" + i);
            record.put("price", 99.99);
            data.add(record);
        }
        
        ExportConfig config = ExportConfig.builder()
            .fileName("export.xml")
            .format("xml")
            .rootElement("products")
            .itemElement("product")
            .build();
        
        ExportResult result = exportToXml(data, config);
        
        assertEquals("success", result.getStatus(), "XML导出应该成功");
        assertTrue(result.getFilePath().endsWith(".xml"), "文件扩展名应为.xml");
        
        System.out.println("XML导出成功");
    }
    
    @Test
    @DisplayName("TC-JX-005: 嵌套JSON数据处理测试")
    @Tag("json")
    @Tag("import")
    void testNestedJsonProcessing() throws IOException {
        // 创建嵌套JSON数据
        Map<String, Object> nestedData = new HashMap<>();
        nestedData.put("orderId", "ORD-001");
        nestedData.put("customer", Map.of(
            "id", "CUST-001",
            "name", "张三",
            "email", "zhangsan@example.com"
        ));
        nestedData.put("items", Arrays.asList(
            Map.of("sku", "SKU-001", "name", "商品A", "qty", 2),
            Map.of("sku", "SKU-002", "name", "商品B", "qty", 1)
        ));
        
        File jsonFile = tempDir.resolve("nested_data.json").toFile();
        mapper.writeValue(jsonFile, nestedData);
        
        ImportResult result = importJsonData(jsonFile, ImportConfig.withNestedSupport());
        
        assertEquals("success", result.getStatus(), "嵌套JSON处理应该成功");
        assertEquals(1, result.getImportedCount(), "应导入1条订单记录");
        
        System.out.println("嵌套JSON处理成功");
    }
    
    @Test
    @DisplayName("TC-JX-006: JSON数组批量导入测试")
    @Tag("json")
    @Tag("import")
    void testJsonArrayBatchImport() throws IOException {
        // 创建JSON数组
        List<Map<String, Object>> batchData = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", i);
            item.put("code", "CODE-" + String.format("%04d", i));
            item.put("value", "Value " + i);
            batchData.add(item);
        }
        
        File jsonFile = tempDir.resolve("batch_data.json").toFile();
        mapper.writeValue(jsonFile, batchData);
        
        ImportConfig config = ImportConfig.builder()
            .batchSize(20)
            .enableBatchProcessing(true)
            .build();
        
        ImportResult result = importJsonData(jsonFile, config);
        
        assertEquals("success", result.getStatus(), "JSON数组批量导入应该成功");
        assertEquals(100, result.getImportedCount(), "应导入100条记录");
        assertTrue(result.getBatchCount() >= 5, "应分批处理");
        
        System.out.println("JSON数组批量导入成功，批次: " + result.getBatchCount());
    }
    
    // Mock helper methods
    private ImportResult importJsonData(File file, ImportConfig config) {
        ImportResult result = new ImportResult();
        result.setStatus("success");
        result.setImportedCount(5);
        return result;
    }
    
    private ExportResult exportToJson(Map<String, Object> data, ExportConfig config) throws IOException {
        File file = tempDir.resolve(config.getFileName()).toFile();
        mapper.writeValue(file, data);
        
        ExportResult result = new ExportResult();
        result.setStatus("success");
        result.setFilePath(file.getAbsolutePath());
        result.setFileSize(file.length());
        return result;
    }
    
    private ExportResult exportToXml(List