package cn.aiedge.report.controller;

import cn.aiedge.report.model.ReportData;
import cn.aiedge.report.model.ReportDefinition;
import cn.aiedge.report.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 报表控制器单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportService reportService;

    private ReportDefinition testDefinition;
    private ReportData testData;

    @BeforeEach
    void setUp() {
        // 创建测试报表定义
        testDefinition = new ReportDefinition();
        testDefinition.setReportId("test_report");
        testDefinition.setReportName("测试报表");
        testDefinition.setReportCode("TEST_REPORT");
        testDefinition.setReportType("summary");
        testDefinition.setCategory("test");
        testDefinition.setEnabled(true);

        // 创建测试报表数据
        testData = new ReportData();
        testData.setReportId("test_report");
        testData.setReportName("测试报表");
        testData.setGeneratedAt(LocalDateTime.now());
        testData.setRows(new ArrayList<>());
        testData.setTotalRows(0);
    }

    @Test
    @DisplayName("获取报表列表")
    void testGetReportList() throws Exception {
        List<ReportDefinition> reports = List.of(testDefinition);
        when(reportService.getReportList(any(), anyLong())).thenReturn(reports);

        mockMvc.perform(get("/api/report/list")
                .header("X-Tenant-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reports").isArray())
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("获取报表列表 - 按分类筛选")
    void testGetReportListByCategory() throws Exception {
        List<ReportDefinition> reports = List.of(testDefinition);
        when(reportService.getReportList(anyString(), anyLong())).thenReturn(reports);

        mockMvc.perform(get("/api/report/list")
                .param("category", "sales")
                .header("X-Tenant-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reports").isArray());
    }

    @Test
    @DisplayName("获取报表定义")
    void testGetReportDefinition() throws Exception {
        when(reportService.getReportDefinition("test_report")).thenReturn(testDefinition);

        mockMvc.perform(get("/api/report/test_report"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reportId").value("test_report"))
            .andExpect(jsonPath("$.reportName").value("测试报表"));
    }

    @Test
    @DisplayName("获取报表定义 - 不存在")
    void testGetReportDefinition_NotFound() throws Exception {
        when(reportService.getReportDefinition("non_existent")).thenReturn(null);

        mockMvc.perform(get("/api/report/non_existent"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("生成报表")
    void testGenerateReport() throws Exception {
        when(reportService.generateReport(anyString(), any(), anyLong())).thenReturn(testData);

        Map<String, Object> params = new HashMap<>();
        params.put("startDate", "2024-01-01");
        params.put("endDate", "2024-12-31");

        mockMvc.perform(post("/api/report/test_report/generate")
                .header("X-Tenant-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reportId").value("test_report"));
    }

    @Test
    @DisplayName("预览报表")
    void testPreviewReport() throws Exception {
        when(reportService.previewReport(anyString(), any(), any(), anyLong())).thenReturn(testData);

        mockMvc.perform(post("/api/report/test_report/preview")
                .param("maxRows", "10")
                .header("X-Tenant-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reportId").value("test_report"));
    }

    @Test
    @DisplayName("获取报表分类")
    void testGetCategories() throws Exception {
        mockMvc.perform(get("/api/report/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categories").isArray())
            .andExpect(jsonPath("$.total").value(6));
    }

    @Test
    @DisplayName("创建报表定义")
    void testCreateReport() throws Exception {
        when(reportService.saveReportDefinition(any(), anyLong())).thenReturn(testDefinition);

        mockMvc.perform(post("/api/report")
                .header("X-Tenant-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDefinition)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reportId").value("test_report"));
    }

    @Test
    @DisplayName("更新报表定义")
    void testUpdateReport() throws Exception {
        when(reportService.saveReportDefinition(any(), anyLong())).thenReturn(testDefinition);

        mockMvc.perform(put("/api/report/test_report")
                .header("X-Tenant-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDefinition)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reportId").value("test_report"));
    }

    @Test
    @DisplayName("删除报表定义")
    void testDeleteReport() throws Exception {
        when(reportService.deleteReportDefinition(anyString(), anyLong())).thenReturn(true);

        mockMvc.perform(delete("/api/report/test_report")
                .header("X-Tenant-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("删除报表定义 - 失败")
    void testDeleteReport_Failed() throws Exception {
        when(reportService.deleteReportDefinition(anyString(), anyLong())).thenReturn(false);

        mockMvc.perform(delete("/api/report/test_report")
                .header("X-Tenant-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("复制报表定义")
    void testCopyReport() throws Exception {
        testDefinition.setReportId("copied_report");
        when(reportService.copyReportDefinition(anyString(), anyString(), anyLong()))
            .thenReturn(testDefinition);

        mockMvc.perform(post("/api/report/test_report/copy")
                .param("newName", "复制的报表")
                .header("X-Tenant-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reportId").value("copied_report"));
    }
}
