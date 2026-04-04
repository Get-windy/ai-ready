package cn.aiedge.export;

import cn.aiedge.export.controller.DataExportController;
import cn.aiedge.export.controller.DataExportControllerExt;
import cn.aiedge.export.service.DataExportService;
import cn.aiedge.export.service.PdfExportService;
import cn.aiedge.export.service.impl.DataExportServiceImpl;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 数据导出模块单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataExportModuleTest {

    @Mock
    private DataExportService dataExportService;

    @Mock
    private PdfExportService pdfExportService;

    @InjectMocks
    private DataExportController dataExportController;

    @InjectMocks
    private DataExportControllerExt dataExportControllerExt;

    @InjectMocks
    private DataExportServiceImpl dataExportServiceImpl;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // ==================== Excel 导出测试 ====================

    @Test
    @Order(1)
    @DisplayName("Excel导出 - 正常导出")
    void testExportExcelSuccess() throws Exception {
        List<TestUser> data = createTestUsers(10);
        Map<String, String> headers = createTestHeaders();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        dataExportServiceImpl.exportExcel(data, headers, out);

        assertTrue(out.size() > 0, "导出的Excel文件不应为空");
    }

    @Test
    @Order(2)
    @DisplayName("Excel导出 - 空数据列表")
    void testExportExcelEmptyData() throws Exception {
        List<TestUser> data = Collections.emptyList();
        Map<String, String> headers = createTestHeaders();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        dataExportServiceImpl.exportExcel(data, headers, out);

        assertTrue(out.size() > 0, "空数据导出应生成只有表头的Excel");
    }

    @Test
    @Order(3)
    @DisplayName("Excel导出 - 空表头")
    void testExportExcelEmptyHeaders() {
        List<TestUser> data = createTestUsers(5);
        Map<String, String> headers = Collections.emptyMap();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertThrows(IllegalArgumentException.class, () -> 
            dataExportServiceImpl.exportExcel(data, null, out)
        );
    }

    @Test
    @Order(4)
    @DisplayName("Excel批量导出 - 正常导出")
    void testExportExcelBatchSuccess() throws Exception {
        Map<String, String> headers = createTestHeaders();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataExportService.DataProvider<TestUser> provider = (page, size) -> {
            if (page <= 3) {
                return createTestUsers(size);
            }
            return Collections.emptyList();
        };

        dataExportServiceImpl.exportExcelBatch(provider, headers, out, 100);

        assertTrue(out.size() > 0, "批量导出的Excel文件不应为空");
    }

    @Test
    @Order(5)
    @DisplayName("Excel批量导出 - 大数据量")
    void testExportExcelBatchLargeData() throws Exception {
        Map<String, String> headers = createTestHeaders();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AtomicInteger totalFetched = new AtomicInteger(0);

        DataExportService.DataProvider<TestUser> provider = (page, size) -> {
            int fetched = totalFetched.getAndAdd(size);
            if (fetched < 10000) {
                return createTestUsers(size);
            }
            return Collections.emptyList();
        };

        dataExportServiceImpl.exportExcelBatch(provider, headers, out, 500);

        assertTrue(out.size() > 0, "大数据量导出应成功");
    }

    // ==================== CSV 导出测试 ====================

    @Test
    @Order(10)
    @DisplayName("CSV导出 - 正常导出")
    void testExportCsvSuccess() throws Exception {
        List<TestUser> data = createTestUsers(10);
        Map<String, String> headers = createTestHeaders();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        dataExportServiceImpl.exportCsv(data, headers, out);

        String content = out.toString("UTF-8");
        assertTrue(content.contains("用户名"), "CSV应包含表头");
        assertTrue(content.contains("test"), "CSV应包含数据");
    }

    @Test
    @Order(11)
    @DisplayName("CSV导出 - 中文支持")
    void testExportCsvChineseSupport() throws Exception {
        List<TestUser> data = new ArrayList<>();
        data.add(new TestUser(1L, "张三", "zhangsan@test.com", "13800138000"));

        Map<String, String> headers = createTestHeaders();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        dataExportServiceImpl.exportCsv(data, headers, out);

        String content = out.toString("UTF-8");
        assertTrue(content.contains("张三"), "CSV应正确显示中文");
    }

    @Test
    @Order(12)
    @DisplayName("CSV导出 - 特殊字符处理")
    void testExportCsvSpecialCharacters() throws Exception {
        List<TestUser> data = new ArrayList<>();
        data.add(new TestUser(1L, "test,user", "test@test.com", "13800138000"));
        data.add(new TestUser(2L, "test\"user", "test2@test.com", "13800138001"));

        Map<String, String> headers = createTestHeaders();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        dataExportServiceImpl.exportCsv(data, headers, out);

        assertTrue(out.size() > 0, "包含特殊字符的CSV应正确导出");
    }

    // ==================== Excel 导入测试 ====================

    @Test
    @Order(20)
    @DisplayName("Excel导入 - 正常导入")
    void testImportExcelSuccess() throws Exception {
        // 先导出再导入
        List<TestUser> exportData = createTestUsers(5);
        Map<String, String> headers = createTestHeaders();

        ByteArrayOutputStream exportOut = new ByteArrayOutputStream();
        dataExportServiceImpl.exportExcel(exportData, headers, exportOut);

        InputStream importIn = new ByteArrayInputStream(exportOut.toByteArray());
        List<TestUser> importData = dataExportServiceImpl.importExcel(importIn, headers, TestUser.class);

        assertEquals(5, importData.size(), "导入数据数量应一致");
    }

    @Test
    @Order(21)
    @DisplayName("Excel导入 - 空文件")
    void testImportExcelEmptyFile() throws Exception {
        Map<String, String> headers = createTestHeaders();

        // 创建只有表头的Excel
        ByteArrayOutputStream exportOut = new ByteArrayOutputStream();
        dataExportServiceImpl.exportExcel(Collections.emptyList(), headers, exportOut);

        InputStream importIn = new ByteArrayInputStream(exportOut.toByteArray());
        List<TestUser> importData = dataExportServiceImpl.importExcel(importIn, headers, TestUser.class);

        assertEquals(0, importData.size(), "空文件导入应返回空列表");
    }

    @Test
    @Order(22)
    @DisplayName("Excel批量导入 - 正常导入")
    void testImportExcelBatchSuccess() throws Exception {
        List<TestUser> exportData = createTestUsers(100);
        Map<String, String> headers = createTestHeaders();

        ByteArrayOutputStream exportOut = new ByteArrayOutputStream();
        dataExportServiceImpl.exportExcel(exportData, headers, exportOut);

        InputStream importIn = new ByteArrayInputStream(exportOut.toByteArray());

        List<List<TestUser>> batches = new ArrayList<>();
        Consumer<List<TestUser>> processor = batches::add;

        DataExportService.ImportResult result = 
            dataExportServiceImpl.importExcelBatch(importIn, headers, TestUser.class, 20, processor);

        assertEquals(100, result.totalCount(), "总数应为100");
        assertEquals(100, result.successCount(), "成功数应为100");
        assertEquals(0, result.failureCount(), "失败数应为0");
    }

    @Test
    @Order(23)
    @DisplayName("Excel带校验导入 - 部分失败")
    void testImportExcelWithValidation() throws Exception {
        List<TestUser> exportData = createTestUsers(10);
        // 设置一些无效数据
        exportData.get(0).setUsername(""); // 空用户名
        exportData.get(5).setEmail("invalid-email"); // 无效邮箱

        Map<String, String> headers = createTestHeaders();

        ByteArrayOutputStream exportOut = new ByteArrayOutputStream();
        dataExportServiceImpl.exportExcel(exportData, headers, exportOut);

        InputStream importIn = new ByteArrayInputStream(exportOut.toByteArray());

        DataExportService.DataValidator<TestUser> validator = (user, rowIndex) -> {
            if (user.getUsername() == null || user.getUsername().isEmpty()) {
                return DataExportService.ValidationResult.failure("用户名不能为空");
            }
            if (user.getEmail() != null && !user.getEmail().contains("@")) {
                return DataExportService.ValidationResult.failure("邮箱格式不正确");
            }
            return DataExportService.ValidationResult.success();
        };

        DataExportService.ImportResult result = 
            dataExportServiceImpl.importExcelWithValidation(importIn, headers, TestUser.class, validator);

        assertEquals(10, result.totalCount(), "总数应为10");
        assertTrue(result.failureCount() >= 2, "失败数应大于等于2");
    }

    // ==================== CSV 导入测试 ====================

    @Test
    @Order(30)
    @DisplayName("CSV导入 - 正常导入")
    void testImportCsvSuccess() throws Exception {
        List<TestUser> exportData = createTestUsers(5);
        Map<String, String> headers = createTestHeaders();

        ByteArrayOutputStream exportOut = new ByteArrayOutputStream();
        dataExportServiceImpl.exportCsv(exportData, headers, exportOut);

        InputStream importIn = new ByteArrayInputStream(exportOut.toByteArray());
        List<TestUser> importData = dataExportServiceImpl.importCsv(importIn, headers, TestUser.class);

        assertEquals(5, importData.size(), "导入数据数量应一致");
    }

    // ==================== 边界条件测试 ====================

    @Test
    @Order(40)
    @DisplayName("导出 - null数据处理")
    void testExportWithNullFields() throws Exception {
        List<TestUser> data = new ArrayList<>();
        TestUser user = new TestUser();
        user.setId(1L);
        user.setUsername("test");
        // email和phone为null
        data.add(user);

        Map<String, String> headers = createTestHeaders();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertDoesNotThrow(() -> dataExportServiceImpl.exportExcel(data, headers, out));
    }

    @Test
    @Order(41)
    @DisplayName("批量导出 - null数据提供者")
    void testExportBatchWithNullProvider() {
        Map<String, String> headers = createTestHeaders();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertThrows(IllegalArgumentException.class, () ->
            dataExportServiceImpl.exportExcelBatch(null, headers, out, 100)
        );
    }

    @Test
    @Order(42)
    @DisplayName("导入 - null输入流")
    void testImportWithNullInputStream() {
        Map<String, String> headers = createTestHeaders();

        assertThrows(IllegalArgumentException.class, () ->
            dataExportServiceImpl.importExcel(null, headers, TestUser.class)
        );
    }

    // ==================== 性能测试 ====================

    @Test
    @Order(50)
    @DisplayName("性能测试 - 大数据量Excel导出")
    void testPerformanceLargeExcelExport() throws Exception {
        List<TestUser> data = createTestUsers(10000);
        Map<String, String> headers = createTestHeaders();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        long startTime = System.currentTimeMillis();
        dataExportServiceImpl.exportExcel(data, headers, out);
        long endTime = System.currentTimeMillis();

        assertTrue(out.size() > 0, "大数据量导出应成功");
        assertTrue(endTime - startTime < 5000, "导出10000条数据应在5秒内完成");
    }

    // ==================== 辅助方法 ====================

    private List<TestUser> createTestUsers(int count) {
        List<TestUser> users = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            users.add(new TestUser(
                    (long) i,
                    "test" + i,
                    "test" + i + "@test.com",
                    "1380013800" + String.format("%02d", i % 100)
            ));
        }
        return users;
    }

    private Map<String, String> createTestHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("id", "ID");
        headers.put("username", "用户名");
        headers.put("email", "邮箱");
        headers.put("phone", "手机号");
        return headers;
    }

    // ==================== 测试实体 ====================

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class TestUser {
        private Long id;
        private String username;
        private String email;
        private String phone;
    }
}
