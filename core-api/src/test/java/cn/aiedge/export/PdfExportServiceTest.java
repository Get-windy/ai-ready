package cn.aiedge.export;

import cn.aiedge.export.service.PdfExportService;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PDF导出服务测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PdfExportServiceTest {

    @InjectMocks
    private PdfExportService pdfExportService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // ==================== PDF 导出测试 ====================

    @Test
    @Order(1)
    @DisplayName("PDF导出 - 正常导出")
    void testExportPdfSuccess() throws Exception {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", "测试" + i);
            row.put("value", i * 100);
            data.add(row);
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("id", "ID");
        headers.put("name", "名称");
        headers.put("value", "数值");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        pdfExportService.exportPdf(data, headers, "测试报告", out);

        assertTrue(out.size() > 0, "PDF文件不应为空");
        // PDF文件以 %PDF 开头
        byte[] bytes = out.toByteArray();
        assertEquals('%', (char) bytes[0]);
        assertEquals('P', (char) bytes[1]);
        assertEquals('D', (char) bytes[2]);
        assertEquals('F', (char) bytes[3]);
    }

    @Test
    @Order(2)
    @DisplayName("PDF导出 - 空数据")
    void testExportPdfEmptyData() throws Exception {
        List<Map<String, Object>> data = Collections.emptyList();

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("id", "ID");
        headers.put("name", "名称");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        pdfExportService.exportPdf(data, headers, "空数据报告", out);

        assertTrue(out.size() > 0, "空数据PDF应成功生成");
    }

    @Test
    @Order(3)
    @DisplayName("PDF导出 - 无标题")
    void testExportPdfNoTitle() throws Exception {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        row.put("name", "测试");
        data.add(row);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("id", "ID");
        headers.put("name", "名称");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        pdfExportService.exportPdf(data, headers, null, out);

        assertTrue(out.size() > 0, "无标题PDF应成功生成");
    }

    @Test
    @Order(4)
    @DisplayName("PDF报告 - 简单文本报告")
    void testExportReportSimple() throws Exception {
        String content = "这是一段测试报告内容。\n包含多行文本。\n测试PDF导出功能。";

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        pdfExportService.exportReport(content, "测试报告", out);

        assertTrue(out.size() > 0, "PDF报告应成功生成");
    }

    @Test
    @Order(5)
    @DisplayName("PDF报告 - 长文本报告")
    void testExportReportLongContent() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("这是第").append(i + 1).append("行测试内容。\n");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        pdfExportService.exportReport(sb.toString(), "长文本报告", out);

        assertTrue(out.size() > 0, "长文本PDF报告应成功生成");
    }

    @Test
    @Order(6)
    @DisplayName("PDF导出 - 中文支持")
    void testExportPdfChineseSupport() throws Exception {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        row.put("name", "张三");
        row.put("department", "技术研发部");
        row.put("position", "高级工程师");
        data.add(row);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("id", "编号");
        headers.put("name", "姓名");
        headers.put("department", "部门");
        headers.put("position", "职位");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        pdfExportService.exportPdf(data, headers, "员工信息表", out);

        assertTrue(out.size() > 0, "中文PDF应成功生成");
    }

    @Test
    @Order(7)
    @DisplayName("PDF导出 - 多列表格")
    void testExportPdfWideTable() throws Exception {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> row = new HashMap<>();
            for (int j = 1; j <= 10; j++) {
                row.put("col" + j, "值" + i + "-" + j);
            }
            data.add(row);
        }

        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i <= 10; i++) {
            headers.put("col" + i, "列" + i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        pdfExportService.exportPdf(data, headers, "宽表测试", out);

        assertTrue(out.size() > 0, "多列PDF应成功生成");
    }

    @Test
    @Order(8)
    @DisplayName("PDF导出 - 特殊字符")
    void testExportPdfSpecialCharacters() throws Exception {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        row.put("description", "包含特殊字符: <>&\"'以及中文");
        data.add(row);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("id", "ID");
        headers.put("description", "描述");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        pdfExportService.exportPdf(data, headers, "特殊字符测试", out);

        assertTrue(out.size() > 0, "特殊字符PDF应成功生成");
    }

    @Test
    @Order(9)
    @DisplayName("PDF导出 - 数值类型")
    void testExportPdfNumericTypes() throws Exception {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        row.put("intValue", 12345);
        row.put("doubleValue", 12345.67);
        row.put("longValue", 9999999999L);
        data.add(row);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("id", "ID");
        headers.put("intValue", "整数");
        headers.put("doubleValue", "浮点数");
        headers.put("longValue", "长整数");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        pdfExportService.exportPdf(data, headers, "数值类型测试", out);

        assertTrue(out.size() > 0, "数值类型PDF应成功生成");
    }

    @Test
    @Order(10)
    @DisplayName("PDF报告 - 空内容")
    void testExportReportEmptyContent() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        pdfExportService.exportReport("", "空报告", out);

        assertTrue(out.size() > 0, "空内容PDF报告应成功生成");
    }

    // ==================== 性能测试 ====================

    @Test
    @Order(20)
    @DisplayName("性能测试 - 大数据量PDF导出")
    void testPerformanceLargePdfExport() throws Exception {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", "测试" + i);
            row.put("value", i * 10);
            data.add(row);
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("id", "ID");
        headers.put("name", "名称");
        headers.put("value", "数值");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        long startTime = System.currentTimeMillis();
        pdfExportService.exportPdf(data, headers, "性能测试报告", out);
        long endTime = System.currentTimeMillis();

        assertTrue(out.size() > 0, "大数据量PDF应成功生成");
        assertTrue(endTime - startTime < 10000, "导出1000条数据应在10秒内完成");
    }
}
