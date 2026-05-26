package com.ai.test.automation;

import org.junit.jupiter.api.*;
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

import java.util.Base64;

/**
 * 电子签收模块自动化测试
 * 任务: task_1776058470024_dfs1o1w96
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestElectronicSignature {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long signatureId;
    private static String orderNo = "ORD202604130001";
    private static final String BASE_URL = "/api/signature";

    private byte[] createTestImage() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    }

    private String createTestSignature() {
        return Base64.getEncoder().encodeToString("测试签名数据".getBytes());
    }

    @Test
    @Order(1)
    @DisplayName("ES-001: 拍照签收-正向流程")
    public void testPhotoSignature() throws Exception {
        MockMultipartFile photo = new MockMultipartFile(
            "photo", "receipt-photo.jpg", "image/jpeg", createTestImage()
        );

        MvcResult result = mockMvc.perform(multipart(BASE_URL + "/photo")
                .file(photo)
                .param("orderNo", orderNo)
                .param("signerName", "张三")
                .param("signerPhone", "13800138000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.signatureType").value("PHOTO"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response);
        signatureId = responseJson.get("data").get("id").asLong();
    }

    @Test
    @Order(2)
    @DisplayName("ES-002: 电子签名签收-正向流程")
    public void testElectronicSignature() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("orderNo", "ORD202604130002");
        request.put("signerName", "李四");
        request.put("signatureData", createTestSignature());
        request.put("signatureType", "ELECTRONIC");

        mockMvc.perform(post(BASE_URL + "/electronic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.signatureType").value("ELECTRONIC"));
    }

    @Test
    @Order(3)
    @DisplayName("ES-003: 签收信息查询")
    public void testQuerySignature() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + signatureId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(signatureId));
    }

    @Test
    @Order(4)
    @DisplayName("ES-004: 签收防篡改验证")
    public void testVerifyIntegrity() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + signatureId + "/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.verified").value(true));
    }

    @Test
    @Order(5)
    @DisplayName("ES-005: 签收记录列表查询")
    public void testQueryList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/list")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("ES-006: 空签名数据验证")
    public void testEmptySignature() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("orderNo", "ORD202604130003");
        request.put("signatureData", "");

        mockMvc.perform(post(BASE_URL + "/electronic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @RepeatedTest(5)
    @Order(7)
    @DisplayName("ES-007: 图片上传压力测试")
    public void testPhotoUploadStress() throws Exception {
        MockMultipartFile photo = new MockMultipartFile(
            "photo", "stress-test.jpg", "image/jpeg", createTestImage()
        );

        mockMvc.perform(multipart(BASE_URL + "/photo")
                .file(photo)
                .param("orderNo", "ORD" + System.currentTimeMillis())
                .param("signerName", "压力测试"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200));
    }
}
