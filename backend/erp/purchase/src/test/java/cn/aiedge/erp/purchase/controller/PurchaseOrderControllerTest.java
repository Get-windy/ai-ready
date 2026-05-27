package cn.aiedge.erp.purchase.controller;

import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.enums.OrderStatus;
import cn.aiedge.erp.purchase.service.PurchaseOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PurchaseOrderController.class)
class PurchaseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PurchaseOrderService purchaseOrderService;

    private PurchaseOrder testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new PurchaseOrder();
        testOrder.setId(1L);
        testOrder.setOrderNo("PO-20260505-001");
        testOrder.setTenantId(1L);
        testOrder.setSupplierId(100L);
        testOrder.setSupplierName("测试供应商有限公司");
        testOrder.setTotalAmount(new BigDecimal("50000.00"));
        testOrder.setTaxAmount(new BigDecimal("6500.00"));
        testOrder.setTotalAmountWithTax(new BigDecimal("56500.00"));
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder.setExpectedDate(LocalDateTime.now().plusDays(7));
        testOrder.setStatus(OrderStatus.PENDING_APPROVAL);
    }

    @Test
    @DisplayName("PM-ORD-001: 创建采购订单测试")
    void testCreatePurchaseOrder() throws Exception {
        PurchaseOrder newOrder = new PurchaseOrder();
        newOrder.setSupplierId(100L);
        newOrder.setTotalAmount(new BigDecimal("30000.00"));

        when(purchaseOrderService.createOrder(any(PurchaseOrder.class))).thenReturn(1L);

        mockMvc.perform(post("/api/erp/purchase/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newOrder)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PM-ORD-002: 获取采购订单详情测试")
    void testGetPurchaseOrderById() throws Exception {
        when(purchaseOrderService.getOrderDetail(1L)).thenReturn(testOrder);

        mockMvc.perform(get("/api/erp/purchase/order/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderNo").value("PO-20260505-001"))
                .andExpect(jsonPath("$.data.supplierName").value("测试供应商有限公司"));
    }

    @Test
    @DisplayName("PM-ORD-003: 更新采购订单测试")
    void testUpdatePurchaseOrder() throws Exception {
        PurchaseOrder updateOrder = new PurchaseOrder();
        updateOrder.setId(1L);
        updateOrder.setTotalAmount(new BigDecimal("35000.00"));

        mockMvc.perform(put("/api/erp/purchase/order/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateOrder)))
                .andExpect(status().isOk());
        
        verify(purchaseOrderService).updateOrder(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("PM-ORD-004: 提交订单审批测试")
    void testSubmitOrderForApproval() throws Exception {
        PurchaseOrder submittedOrder = new PurchaseOrder();
        submittedOrder.setId(1L);
        submittedOrder.setStatus(OrderStatus.PENDING_APPROVAL);
        
        when(purchaseOrderService.submitOrder(1L)).thenReturn(submittedOrder);

        mockMvc.perform(post("/api/erp/purchase/order/{id}/submit", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PM-ORD-005: 审批采购订单测试")
    void testApprovePurchaseOrder() throws Exception {
        PurchaseOrder approvedOrder = new PurchaseOrder();
        approvedOrder.setId(1L);
        approvedOrder.setStatus(OrderStatus.APPROVED);
        
        when(purchaseOrderService.approveOrder(eq(1L), anyLong(), anyString())).thenReturn(approvedOrder);

        mockMvc.perform(post("/api/erp/purchase/order/{id}/approve", 1L)
                .param("approverId", "1")
                .param("comment", "测试审批意见"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PM-ORD-006: 驳回采购订单测试")
    void testRejectPurchaseOrder() throws Exception {
        mockMvc.perform(post("/api/erp/purchase/order/{id}/reject", 1L)
                .param("reason", "测试驳回原因"))
                .andExpect(status().isOk());
        
        verify(purchaseOrderService).reject(eq(1L), anyString());
    }

    @Test
    @DisplayName("PM-ORD-007: 取消采购订单测试")
    void testCancelPurchaseOrder() throws Exception {
        mockMvc.perform(delete("/api/erp/purchase/order/{id}", 1L)
                .param("reason", "测试取消原因"))
                .andExpect(status().isOk());
        
        verify(purchaseOrderService).cancel(eq(1L), anyString());
    }

    @Test
    @DisplayName("PM-ORD-008: 查询采购订单列表测试")
    void testGetPurchaseOrderList() throws Exception {
        mockMvc.perform(get("/api/erp/purchase/order")
                .param("page", "1")
                .param("size", "10")
                .param("status", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PM-ORD-009: 供应商确认订单测试")
    void testSupplierConfirmOrder() throws Exception {
        mockMvc.perform(post("/api/erp/purchase/order/{id}/supplier-confirm", 1L))
                .andExpect(status().isOk());
        
        verify(purchaseOrderService).confirmBySupplier(1L);
    }

    @Test
    @DisplayName("PM-ORD-010: 下达订单测试")
    void testIssueOrder() throws Exception {
        PurchaseOrder issuedOrder = new PurchaseOrder();
        issuedOrder.setId(1L);
        issuedOrder.setStatus(OrderStatus.ISSUED);
        
        when(purchaseOrderService.issueOrder(1L)).thenReturn(issuedOrder);

        mockMvc.perform(post("/api/erp/purchase/order/{id}/issue", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("异常测试：创建订单时供应商不存在")
    void testCreateOrderWithInvalidSupplier() throws Exception {
        PurchaseOrder invalidOrder = new PurchaseOrder();
        invalidOrder.setSupplierId(99999L);
        
        when(purchaseOrderService.createOrder(any(PurchaseOrder.class)))
                .thenThrow(new RuntimeException("供应商不存在"));

        mockMvc.perform(post("/api/erp/purchase/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidOrder)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("边界测试：创建超大金额订单")
    void testCreateOrderWithLargeAmount() throws Exception {
        PurchaseOrder largeAmountOrder = new PurchaseOrder();
        largeAmountOrder.setSupplierId(100L);
        largeAmountOrder.setTotalAmount(new BigDecimal("999999999.99"));
        
        when(purchaseOrderService.createOrder(any(PurchaseOrder.class))).thenReturn(2L);

        mockMvc.perform(post("/api/erp/purchase/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeAmountOrder)))
                .andExpect(status().isOk());
    }
}