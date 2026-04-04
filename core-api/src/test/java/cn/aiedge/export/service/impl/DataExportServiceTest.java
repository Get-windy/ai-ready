package cn.aiedge.export.service.impl;

import cn.aiedge.export.service.DataExportService;
import cn.hutool.core.bean.BeanUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataExportService 单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
class DataExportServiceTest {

    private DataExportService dataExportService;

    @BeforeEach
    void setUp() {
        dataExportService = new DataExportServiceImpl();
    }

    @Test
    @DisplayName("导出Excel - 成功")
    void testExportExcel_Success() throws Exception {
        // Given
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(Map.of("name", "张三", "age", 25, "email", "zhangsan@test.com"));
        data.add(Map.of("name", "李四", "age", 30, "email", "lisi@test.com"));

        Map<String, String> headers = Map.of("name", "姓名", "age", "年龄", "email", "邮箱");

        // When
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        dataExportService.exportExcel(data, headers, out);

        // Then
        assertTrue(out.size() > 0);
        // 验证Excel内容（简化验证）
    }

    @Test
    @DisplayName("导出CSV - 成功")
    void testExportCsv_Success() throws Exception {
        // Given
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(Map.of("name", "张三", "age", 25));
        data.add(Map.of("name", "李四", "age", 30));

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("name", "姓名");
        headers.put("age", "年龄");

        // When
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        dataExportService.exportCsv(data, headers, out);

        // Then
        String csv = out.toString(StandardCharsets.UTF_8);
        assertTrue(csv.contains("姓名"));
        assertTrue(csv.contains("年龄"));
        assertTrue(csv.contains("张三"));
        assertTrue(csv.contains("李四"));
    }

    @Test
    @DisplayName("导入CSV - 成功")
    void testImportCsv_Success() throws Exception {
        // Given
        String csvContent = "\ufeff姓名,年龄\n张三,25\n李四,30\n";
        InputStream in = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("name", "姓名");
        headers.put("age", "年龄");

        // When
        List<Map> result = dataExportService.importCsv(in, headers, Map.class);

        // Then
        assertEquals(2, result.size());
        assertEquals("张三", result.get(0).get("name"));
        assertEquals("李四", result.get(1).get("name"));
    }

    @Test
    @DisplayName("导入CSV - 包含逗号的字段")
    void testImportCsv_WithComma() throws Exception {
        // Given
        String csvContent = "name,description\n" +
                "张三,\"这是,包含逗号的描述\"\n";
        InputStream in = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = Map.of("name", "name", "description", "description");

        // When
        List<Map> result = dataExportService.importCsv(in, headers, Map.class);

        // Then
        assertEquals(1, result.size());
        assertEquals("张三", result.get(0).get("name"));
        assertEquals("这是,包含逗号的描述", result.get(0).get("description"));
    }

    @Test
    @DisplayName("导出CSV - 特殊字符转义")
    void testExportCsv_SpecialCharacters() throws Exception {
        // Given
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(Map.of("name", "张三", "desc", "包含\"引号\"的内容"));

        Map<String, String> headers = Map.of("name", "姓名", "desc", "描述");

        // When
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        dataExportService.exportCsv(data, headers, out);

        // Then
        String csv = out.toString(StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"包含\"\"引号\"\"的内容\""));
    }

    @Test
    @DisplayName("批量导入 - 成功")
    void testImportExcelBatch_Success() throws Exception {
        // Given
        String csvContent = "name,age\n张三,25\n李四,30\n王五,35\n";
        InputStream in = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = Map.of("name", "name", "age", "age");

        // When
        List<List<Map>> batches = new ArrayList<>();
        DataExportService.ImportResult result = dataExportService.importExcelBatch(
                in, headers, Map.class, 2,
                batches::add
        );

        // Then
        assertEquals(3, result.totalCount());
        assertEquals(2, batches.size()); // 2条 + 1条
    }

    @Test
    @DisplayName("导入并校验 - 部分失败")
    void testImportExcelWithValidation() throws Exception {
        // Given
        String csvContent = "name,age\n张三,25\n,30\n王五,-1\n";
        InputStream in = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = Map.of("name", "name", "age", "age");

        // When
        DataExportService.ImportResult result = dataExportService.importExcelWithValidation(
                in, headers, Map.class,
                (data, rowIndex) -> {
                    Map<String, Object> map = (Map<String, Object>) data;
                    String name = (String) map.get("name");
                    Integer age = Integer.parseInt(String.valueOf(map.get("age")));

                    if (name == null || name.isEmpty()) {
                        return DataExportService.ValidationResult.failure("姓名不能为空");
                    }
                    if (age < 0) {
                        return DataExportService.ValidationResult.failure("年龄不能为负数");
                    }
                    return DataExportService.ValidationResult.success();
                }
        );

        // Then
        assertEquals(3, result.totalCount());
        assertEquals(1, result.successCount());
        assertEquals(2, result.failureCount());
    }
}
