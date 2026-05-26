package com.ai.test.automation;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * 远程打印模块自动化测试
 * 任务: task_1776058469930_ubo2fzlr7
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestRemotePrinting {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long templateId;
    private static String printJobId;
    private static final String BASE_URL = "/api/printing";

    private ObjectNode createTemplateData(String name, String type) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("templateName", name);
        data.put("templateType", type);
        data.put("status", "ACTIVE");
        return data;
    }

    @Test
    @Order(1)
    @DisplayName("RP-001: 上传打印模板")
    public void testUploadTemplate() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test-template.docx", 
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "测试模板内容".getBytes()
        );

        MvcResult result = mockMvc.perform(multipart(BASE_URL + "/templates")
                .file(file)
                .param("templateName", "销售订单模板")
                .param("templateType", "订单打印"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response);
        templateId = responseJson.get("data").get("id").asLong();
    }

    @Test
    @Order(2)
    @DisplayName("RP-002: 下载打印模板")
    public void testDownloadTemplate() throws Exception {
        mockMvc.perform(get(BASE_URL + "/templates/" + templateId + "/download"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(3)
    @DisplayName("RP-003: 发起打印请求")
    public void testCreatePrintJob() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("templateId", templateId);
        request.put("printerId", "PRINTER_001");
        request.put("copies", 1);

        ObjectNode printData = objectMapper.createObjectNode();
        printData.put("orderNo", "ORD202604130001");
        printData.put("customerName", "测试客户");
        request.set("printData", printData);

        MvcResult result = mockMvc.perform(post(BASE_URL + "/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response);
        printJobId = responseJson.get("data").get("jobId").asText();
    }

    @Test
    @Order(4)
    @DisplayName("RP-004: 查询打印任务状态")
    public void testQueryJobStatus() throws Exception {
        mockMvc.perform(get(BASE_URL + "/jobs/" + printJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jobId").value(printJobId));
    }

    @Test
    @Order(5)
    @DisplayName("RP-005: 查询打印任务列表")
    public void testQueryJobList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/jobs")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("RP-006: 取消打印任务")
    public void testCancelJob() throws Exception {
        mockMvc.perform(post(BASE_URL + "/jobs/" + printJobId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @ParameterizedTest
    @CsvSource({
        "PENDING, 待处理",
        "PRINTING, 打印中",
        "COMPLETED, 已完成",
        "FAILED, 失败"
    })
    @Order(7)
    @DisplayName("RP-007: 按状态筛选打印任务")
    public void testQueryByStatus(String status, String desc) throws Exception {
        mockMvc.perform(get(BASE_URL + "/jobs")
                .param("status", status)
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(8)
    @DisplayName("RP-008: 打印机离线错误处理")
    public void testPrinterOffline() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("templateId", templateId);
        request.put("printerId", "PRINTER_OFFLINE");
        request.put("copies", 1);

        mockMvc.perform(post(BASE_URL + "/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @Order(9)
    @DisplayName("RP-009: 批量打印")
    public void testBatchPrint() throws Exception {
        ArrayNode jobs = objectMapper.createArrayNode();
        for (int i = 0; i < 3; i++) {
            ObjectNode job = objectMapper.createObjectNode();
            job.put("templateId", templateId);
            job.put("printerId", "PRINTER_" + i);
            jobs.add(job);
        }

        mockMvc.perform(post(BASE_URL + "/jobs/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jobs.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.successCount").value(3));
    }

    @Test
    @Order(10)
    @DisplayName("RP-010: 删除打印模板")
    public void testDeleteTemplate() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/templates/" + templateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
