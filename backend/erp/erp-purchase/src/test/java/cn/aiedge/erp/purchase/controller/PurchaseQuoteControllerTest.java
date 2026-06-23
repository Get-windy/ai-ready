package cn.aiedge.erp.purchase.controller;

import cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote;
import cn.aiedge.erp.purchase.enums.QuoteStatus;
import cn.aiedge.erp.purchase.service.PurchaseQuoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 采购报价控制器测试类
 * 
 * @author team-member
 * @date 2026-04-29
 */
@WebMvcTest(PurchaseQuoteController.class)
@DisplayName("采购报价管理控制器测试")
public class PurchaseQuoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PurchaseQuoteService quoteService;

    @Autowired
    private ObjectMapper objectMapper;

    private PurchaseSupplierQuote sampleQuote;

    @BeforeEach
    void setUp() {
        sampleQuote = new PurchaseSupplierQuote();
        sampleQuote.setId(1L);
        sampleQuote.setQuoteNo("QUO20260429001");
        sampleQuote.setInquiryId(1L);
        sampleQuote.setSupplierId(50L);
        sampleQuote.setSupplierName("供应商A");
        sampleQuote.setTotalAmount(new BigDecimal("50000"));
        sampleQuote.setQuoteStatus(QuoteStatus.SUBMITTED);
        sampleQuote.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("TC-QUO-001: 提交报价")
    void testSubmitQuote() throws Exception {
        when(quoteService.submitQuote(any(), any())).thenReturn(sampleQuote);

        Map<String, Object> request = new HashMap<>();
        request.put("quote", sampleQuote);
        request.put("items", Arrays.asList());

        mockMvc.perform(post("/api/erp/purchase/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("TC-QUO-002: 更新报价")
    void testUpdateQuote() throws Exception {
        when(quoteService.updateQuote(anyLong(), any())).thenReturn(sampleQuote);

        mockMvc.perform(put("/api/erp/purchase/quote/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleQuote)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-QUO-003: 审查报价（评分）")
    void testReviewQuote() throws Exception {
        when(quoteService.reviewQuote(anyLong(), any(), any(), any(), any())).thenReturn(sampleQuote);

        Map<String, Object> scores = new HashMap<>();
        scores.put("priceScore", 85.0);
        scores.put("qualityScore", 90.0);
        scores.put("serviceScore", 88.0);
        scores.put("reviewComment", "报价合理");

        mockMvc.perform(post("/api/erp/purchase/quote/1/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scores)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-QUO-004: 接受报价")
    void testAcceptQuote() throws Exception {
        PurchaseSupplierQuote accepted = new PurchaseSupplierQuote();
        accepted.setId(1L);
        accepted.setQuoteStatus(QuoteStatus.ACCEPTED);
        when(quoteService.acceptQuote(1L)).thenReturn(accepted);

        mockMvc.perform(post("/api/erp/purchase/quote/1/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("TC-QUO-005: 拒绝报价")
    void testRejectQuote() throws Exception {
        PurchaseSupplierQuote rejected = new PurchaseSupplierQuote();
        rejected.setId(1L);
        rejected.setQuoteStatus(QuoteStatus.REJECTED);
        when(quoteService.rejectQuote(anyLong(), anyString())).thenReturn(rejected);

        Map<String, String> request = new HashMap<>();
        request.put("reason", "价格过高");

        mockMvc.perform(post("/api/erp/purchase/quote/1/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("TC-QUO-006: 撤回报价")
    void testWithdrawQuote() throws Exception {
        PurchaseSupplierQuote withdrawn = new PurchaseSupplierQuote();
        withdrawn.setId(1L);
        withdrawn.setQuoteStatus(QuoteStatus.WITHDRAWN);
        when(quoteService.withdrawQuote(1L)).thenReturn(withdrawn);

        mockMvc.perform(post("/api/erp/purchase/quote/1/withdraw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));
    }

    @Test
    @DisplayName("TC-QUO-007: 查询报价详情")
    void testGetQuoteById() throws Exception {
        when(quoteService.getQuoteById(1L)).thenReturn(sampleQuote);

        mockMvc.perform(get("/api/erp/purchase/quote/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("TC-QUO-008: 按询价单查询报价列表")
    void testGetQuotesByInquiry() throws Exception {
        List<PurchaseSupplierQuote> quotes = Arrays.asList(sampleQuote);
        when(quoteService.getQuotesByInquiry(1L)).thenReturn(quotes);

        mockMvc.perform(get("/api/erp/purchase/quote/inquiry/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("TC-QUO-009: 按供应商查询报价")
    void testGetQuotesBySupplier() throws Exception {
        List<PurchaseSupplierQuote> quotes = Arrays.asList(sampleQuote);
        when(quoteService.getQuotesBySupplier(50L)).thenReturn(quotes);

        mockMvc.perform(get("/api/erp/purchase/quote/supplier/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("TC-QUO-010: 查询中标报价")
    void testGetWinningQuote() throws Exception {
        when(quoteService.getWinningQuote(1L)).thenReturn(sampleQuote);

        mockMvc.perform(get("/api/erp/purchase/quote/inquiry/1/winner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("TC-QUO-011: 比价分析")
    void testCompareQuotes() throws Exception {
        when(quoteService.compareQuotes(1L)).thenReturn("供应商A报价最低，推荐中标");

        mockMvc.perform(get("/api/erp/purchase/quote/inquiry/1/compare"))
                .andExpect(status().isOk())
                .andExpect(content().string("供应商A报价最低，推荐中标"));
    }

    @Test
    @DisplayName("TC-QUO-查询: 按编号查询")
    void testGetQuoteByNo() throws Exception {
        when(quoteService.getQuoteByNo("QUO20260429001")).thenReturn(sampleQuote);

        mockMvc.perform(get("/api/erp/purchase/quote/no/QUO20260429001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteNo").value("QUO20260429001"));
    }
}
