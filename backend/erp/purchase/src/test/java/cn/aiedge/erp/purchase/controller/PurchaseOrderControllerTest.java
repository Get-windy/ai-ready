package cn.aiedge.erp.purchase.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 采购订单控制器测试
 * 测试用例：PM-ORD-001 ~ PM-ORD-010
 * 
 * @author AI-Ready QA Team
 * @since 1.0.0
 */
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
        testOrder.setStatus(cn.aiedge.erp.purchase.enums.OrderStatus.PENDING_APPROVAL);
    }

    @Test
    @DisplayName("PM-ORD-001: 创建采购订单测试")
    void testCreatePurchaseOrder() throws Exception {
        // 准备测试数据
        PurchaseOrder newOrder = new PurchaseOrder();
        newOrder.setSupplierId(100L);
        newOrder.setTotalAmount(new BigDecimal("30000.00"));

        // 模拟服务层返回
        when(purchaseOrderService.createOrder(any(PurchaseOrder.class))).thenReturn(1L);

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建成功"))
                .andExpect(jsonPath("$.data").value(1L));
    }

    @Test
    @DisplayName("PM-ORD-002: 获取采购订单详情测试")
    void testGetPurchaseOrderById() throws Exception {
        // 模拟服务层返回
        when(purchaseOrderService.getById(1L)).thenReturn(testOrder);

        // 执行测试
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
        // 准备更新数据
        PurchaseOrder updateOrder = new PurchaseOrder();

        // 模拟服务层
        doNothing().when(purchaseOrderService).updateOrder(any(PurchaseOrder.class));

        // 执行测试
        mockMvc.perform(put("/api/erp/purchase/order/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新成功"));
    }

    @Test
    @DisplayName("PM-ORD-004: 提交订单审批测试")
    void testSubmitOrderForApproval() throws Exception {
        // 模拟服务层
        doNothing().when(purchaseOrderService).submitForApproval(1L);

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/order/{id}/submit", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("提交审批成功"));
    }

    @Test
    @DisplayName("PM-ORD-005: 审批采购订单测试")
    void testApprovePurchaseOrder() throws Exception {
        // 模拟服务层
        when(purchaseOrderService.approveOrder(1L, 1L, "测试审批意见")).thenReturn(testOrder);

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/order/{id}/approve", 1L)
                .param("comment", "测试审批意见"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("审批通过"));
    }

    @Test
    @DisplayName("PM-ORD-006: 驳回采购订单测试")
    void testRejectPurchaseOrder() throws Exception {
        // 模拟服务层
        doNothing().when(purchaseOrderService).reject(1L, "测试驳回原因");

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/order/{id}/reject", 1L)
                .param("reason", "测试驳回原因"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("订单已驳回"));
    }

    @Test
    @DisplayName("PM-ORD-007: 取消采购订单测试")
    void testCancelPurchaseOrder() throws Exception {
        // 模拟服务层
        doNothing().when(purchaseOrderService).cancel(1L, "测试取消原因");

        // 执行测试
        mockMvc.perform(delete("/api/erp/purchase/order/{id}", 1L)
                .param("reason", "测试取消原因"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("订单已取消"));
    }

    @Test
    @DisplayName("PM-ORD-008: 查询采购订单列表测试")
    void testGetPurchaseOrderList() throws Exception {
        // 执行测试
        mockMvc.perform(get("/api/erp/purchase/order")
                .param("page", "1")
                .param("size", "10")
                .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PM-ORD-009: 供应商确认订单测试")
    void testSupplierConfirmOrder() throws Exception {
        // 模拟服务层
        doNothing().when(purchaseOrderService).updateById(any(PurchaseOrder.class));

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/order/{id}/supplier-confirm", 1L)
                .param("confirmationNote", "供应商确认接收"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("供应商确认成功"));
    }

    @Test
    @DisplayName("PM-ORD-010: 更新订单发货状态测试")
    void testUpdateOrderDeliveryStatus() throws Exception {
        // 模拟服务层
        doNothing().when(purchaseOrderService).updateById(any(PurchaseOrder.class));

        // 执行测试
        mockMvc.perform(put("/api/erp/purchase/order/{id}/delivery-status", 1L)
                .param("status", "2")
                .param("deliveryNote", "已发货50%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("发货状态更新成功"));
    }

    @Test
    @DisplayName("异常测试：创建订单时供应商不存在")
    void testCreateOrderWithInvalidSupplier() throws Exception {
        // 准备测试数据
        PurchaseOrder invalidOrder = new PurchaseOrder();
        invalidOrder.setSupplierId(99999L); // 不存在的供应商
        
        // 模拟服务层抛出异常
        when(purchaseOrderService.createOrder(any(PurchaseOrder.class)))
                .thenThrow(new RuntimeException("供应商不存在"));

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidOrder)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("异常测试：更新不存在的订单")
    void testUpdateNonExistentOrder() throws Exception {
        // 准备更新数据
        PurchaseOrder updateOrder = new PurchaseOrder();

        // 模拟服务层抛出异常
        doThrow(new RuntimeException("订单不存在")).when(purchaseOrderService).updateOrder(any(PurchaseOrder.class));

        // 执行测试
        mockMvc.perform(put("/api/erp/purchase/order/{id}", 99999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateOrder)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("边界测试：创建超大金额订单")
    void testCreateOrderWithLargeAmount() throws Exception {
        // 准备测试数据 - 超大金额
        PurchaseOrder largeAmountOrder = new PurchaseOrder();
        largeAmountOrder.setSupplierId(100L);
        largeAmountOrder.setTotalAmount(new BigDecimal("999999999.99"));
        
        // 模拟服务层
        when(purchaseOrderService.createOrder(any(PurchaseOrder.class))).thenReturn(2L);

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeAmountOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}