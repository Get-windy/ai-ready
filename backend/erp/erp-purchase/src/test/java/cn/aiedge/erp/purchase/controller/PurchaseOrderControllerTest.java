package cn.aiedge.erp.purchase.controller;

import cn.aiedge.base.config.GlobalExceptionHandler;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.purchase.dto.PurchaseOrderDTO;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.entity.PurchaseOrderPartnerSnapshot;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderItemMapper;
import cn.aiedge.erp.purchase.service.PurchaseOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(PurchaseOrderController.class)
@Import(GlobalExceptionHandler.class)
class PurchaseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PurchaseOrderService purchaseOrderService;

    @MockBean
    private PurchaseOrderItemMapper purchaseOrderItemMapper;

    private PurchaseOrder testOrder;
    private PurchaseOrderDTO testOrderDTO;

    @BeforeEach
    void setUp() {
        testOrder = new PurchaseOrder();
        testOrder.setId(1L);
        testOrder.setOrderNo("PO-20260505-001");
        testOrder.setTenantId(1L);
        testOrder.setSupplierId(100L);
        testOrder.setBillAmount(new BigDecimal("50000.00"));
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder.setExpectedReceiveTime(LocalDateTime.now().plusDays(7));
        testOrder.setStatus(1);

        PurchaseOrderPartnerSnapshot snapshot = new PurchaseOrderPartnerSnapshot();
        snapshot.setOrderId(1L);
        snapshot.setSupplierName("测试供应商有限公司");

        testOrderDTO = new PurchaseOrderDTO();
        testOrderDTO.setOrder(testOrder);
        testOrderDTO.setPartnerSnapshot(snapshot);
    }

    @Test
    @DisplayName("PM-ORD-001: 创建采购订单测试")
    void testCreatePurchaseOrder() throws Exception {
        PurchaseOrderDTO newOrder = new PurchaseOrderDTO();
        PurchaseOrder order = new PurchaseOrder();
        order.setSupplierId(100L);
        order.setBillAmount(new BigDecimal("30000.00"));
        newOrder.setOrder(order);

        when(purchaseOrderService.createOrder(any(PurchaseOrderDTO.class))).thenReturn(1L);

        mockMvc.perform(post("/api/erp/purchase/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("PM-ORD-002: 获取采购订单详情测试")
    void testGetPurchaseOrderById() throws Exception {
        when(purchaseOrderService.getOrderDetail(1L)).thenReturn(testOrderDTO);

        mockMvc.perform(get("/api/erp/purchase/order/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.order.id").value(1))
                .andExpect(jsonPath("$.data.order.orderNo").value("PO-20260505-001"))
                .andExpect(jsonPath("$.data.partnerSnapshot.supplierName").value("测试供应商有限公司"));
    }

    @Test
    @DisplayName("PM-ORD-003: 更新采购订单测试")
    void testUpdatePurchaseOrder() throws Exception {
        PurchaseOrderDTO updateOrder = new PurchaseOrderDTO();
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setBillAmount(new BigDecimal("35000.00"));
        updateOrder.setOrder(order);

        mockMvc.perform(put("/api/erp/purchase/order/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateOrder)))
                .andExpect(status().isOk());

        verify(purchaseOrderService).updateOrder(eq(1L), any(PurchaseOrderDTO.class));
    }

    @Test
    @DisplayName("PM-ORD-004: 提交订单审批测试")
    void testSubmitOrderForApproval() throws Exception {
        mockMvc.perform(post("/api/erp/purchase/order/{id}/submit", 1L))
                .andExpect(status().isOk());

        verify(purchaseOrderService).submitForApproval(1L);
    }

    @Test
    @DisplayName("PM-ORD-005: 审批采购订单测试")
    void testApprovePurchaseOrder() throws Exception {
        mockMvc.perform(post("/api/erp/purchase/order/{id}/approve", 1L))
                .andExpect(status().isOk());

        verify(purchaseOrderService).approve(1L);
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
        mockMvc.perform(post("/api/erp/purchase/order/{id}/cancel", 1L)
                .param("reason", "测试取消原因"))
                .andExpect(status().isOk());

        verify(purchaseOrderService).cancel(eq(1L), anyString());
    }

    @Test
    @DisplayName("PM-ORD-008: 查询/导出采购订单列表测试")
    void testGetPurchaseOrderList() throws Exception {
        when(purchaseOrderService.exportOrders(any(), any(), any(), eq(1)))
                .thenReturn(List.of(testOrder));

        mockMvc.perform(get("/api/erp/purchase/order/export")
                .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].orderNo").value("PO-20260505-001"));
    }

    // PM-ORD-009（供应商确认订单）与 PM-ORD-010（下达订单）已删除：
    // 当前 PurchaseOrderController/Service 不再提供 supplier-confirm 与 issue 接口

    @Test
    @DisplayName("异常测试：创建订单时供应商不存在")
    void testCreateOrderWithInvalidSupplier() throws Exception {
        PurchaseOrderDTO invalidOrder = new PurchaseOrderDTO();
        PurchaseOrder order = new PurchaseOrder();
        order.setSupplierId(99999L);
        invalidOrder.setOrder(order);

        when(purchaseOrderService.createOrder(any(PurchaseOrderDTO.class)))
                .thenThrow(BusinessException.badRequest("供应商不存在"));

        mockMvc.perform(post("/api/erp/purchase/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidOrder)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("边界测试：创建超大金额订单")
    void testCreateOrderWithLargeAmount() throws Exception {
        PurchaseOrderDTO largeAmountOrder = new PurchaseOrderDTO();
        PurchaseOrder order = new PurchaseOrder();
        order.setSupplierId(100L);
        order.setBillAmount(new BigDecimal("999999999.99"));
        largeAmountOrder.setOrder(order);

        when(purchaseOrderService.createOrder(any(PurchaseOrderDTO.class))).thenReturn(2L);

        mockMvc.perform(post("/api/erp/purchase/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeAmountOrder)))
                .andExpect(status().isOk());
    }
}
