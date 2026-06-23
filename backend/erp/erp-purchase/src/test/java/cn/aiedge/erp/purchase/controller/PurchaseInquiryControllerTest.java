package cn.aiedge.erp.purchase.controller;

import cn.aiedge.erp.purchase.entity.PurchaseInquiry;
import cn.aiedge.erp.purchase.enums.InquiryStatus;
import cn.aiedge.erp.purchase.service.PurchaseInquiryService;
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
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 采购询价控制器测试类
 * 
 * @author team-member
 * @date 2026-04-29
 */
@WebMvcTest(PurchaseInquiryController.class)
@DisplayName("采购询价管理控制器测试")
public class PurchaseInquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PurchaseInquiryService inquiryService;

    @Autowired
    private ObjectMapper objectMapper;

    private PurchaseInquiry sampleInquiry;

    @BeforeEach
    void setUp() {
        sampleInquiry = new PurchaseInquiry();
        sampleInquiry.setId(1L);
        sampleInquiry.setInquiryNo("INQ20260429001");
        sampleInquiry.setTitle("测试询价单");
        sampleInquiry.setStatus(InquiryStatus.DRAFT);
        sampleInquiry.setPurchaserId(100L);
        sampleInquiry.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("TC-INQ-001: 创建询价单")
    void testCreateInquiry() throws Exception {
        when(inquiryService.createInquiry(any(PurchaseInquiry.class))).thenReturn(sampleInquiry);

        mockMvc.perform(post("/api/erp/purchase/inquiry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleInquiry)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.inquiryNo").value("INQ20260429001"));
    }

    @Test
    @DisplayName("TC-INQ-002: 更新询价单")
    void testUpdateInquiry() throws Exception {
        when(inquiryService.updateInquiry(anyLong(), any(PurchaseInquiry.class))).thenReturn(sampleInquiry);

        mockMvc.perform(put("/api/erp/purchase/inquiry/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleInquiry)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("TC-INQ-003: 发布询价单")
    void testPublishInquiry() throws Exception {
        PurchaseInquiry published = new PurchaseInquiry();
        published.setId(1L);
        published.setStatus(InquiryStatus.PUBLISHED);
        when(inquiryService.publishInquiry(1L)).thenReturn(published);

        mockMvc.perform(post("/api/erp/purchase/inquiry/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("TC-INQ-004: 关闭询价单")
    void testCloseInquiry() throws Exception {
        PurchaseInquiry closed = new PurchaseInquiry();
        closed.setId(1L);
        closed.setStatus(InquiryStatus.CLOSED);
        when(inquiryService.closeInquiry(1L)).thenReturn(closed);

        mockMvc.perform(post("/api/erp/purchase/inquiry/1/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @DisplayName("TC-INQ-005: 取消询价单")
    void testCancelInquiry() throws Exception {
        PurchaseInquiry cancelled = new PurchaseInquiry();
        cancelled.setId(1L);
        cancelled.setStatus(InquiryStatus.CANCELLED);
        when(inquiryService.cancelInquiry(1L)).thenReturn(cancelled);

        mockMvc.perform(post("/api/erp/purchase/inquiry/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("TC-INQ-006: 查询询价单列表")
    void testGetAllInquiries() throws Exception {
        List<PurchaseInquiry> inquiries = Arrays.asList(sampleInquiry);
        when(inquiryService.getAllInquiries()).thenReturn(inquiries);

        mockMvc.perform(get("/api/erp/purchase/inquiry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("TC-INQ-007: 按状态查询询价单")
    void testGetInquiriesByStatus() throws Exception {
        List<PurchaseInquiry> inquiries = Arrays.asList(sampleInquiry);
        when(inquiryService.getInquiriesByStatus("PUBLISHED")).thenReturn(inquiries);

        mockMvc.perform(get("/api/erp/purchase/inquiry/status/PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("TC-INQ-008: 按采购员查询")
    void testGetInquiriesByPurchaser() throws Exception {
        List<PurchaseInquiry> inquiries = Arrays.asList(sampleInquiry);
        when(inquiryService.getInquiriesByPurchaser(100L)).thenReturn(inquiries);

        mockMvc.perform(get("/api/erp/purchase/inquiry/purchaser/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("TC-INQ-009: 删除询价单")
    void testDeleteInquiry() throws Exception {
        doNothing().when(inquiryService).deleteInquiry(1L);

        mockMvc.perform(delete("/api/erp/purchase/inquiry/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-INQ-查询: 查询询价单详情")
    void testGetInquiryById() throws Exception {
        when(inquiryService.getInquiryById(1L)).thenReturn(sampleInquiry);

        mockMvc.perform(get("/api/erp/purchase/inquiry/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("TC-INQ-查询: 按编号查询")
    void testGetInquiryByNo() throws Exception {
        when(inquiryService.getInquiryByNo("INQ20260429001")).thenReturn(sampleInquiry);

        mockMvc.perform(get("/api/erp/purchase/inquiry/no/INQ20260429001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inquiryNo").value("INQ20260429001"));
    }
}
