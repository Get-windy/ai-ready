package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderMapper;
import cn.aiedge.erp.purchase.service.impl.PurchaseOrderServiceImpl;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 采购订单服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderMapper purchaseOrderMapper;

    @InjectMocks
    private PurchaseOrderServiceImpl purchaseOrderService;

    private PurchaseOrder testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new PurchaseOrder();
        testOrder.setTenantId(1L);
        testOrder.setSupplierId(100L);
        testOrder.setSupplierName("测试供应商");
        testOrder.setTotalAmount(new BigDecimal("10000.00"));
        testOrder.setTaxAmount(new BigDecimal("1300.00"));
        testOrder.setTotalAmountWithTax(new BigDecimal("11300.00"));
    }

    @Test
    @DisplayName("创建采购订单 - 成功")
    void testCreateOrder_Success() {
        // Given
        when(purchaseOrderMapper.insert(any(PurchaseOrder.class))).thenReturn(1);

        // Mock StpUtil
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            // When
            Long orderId = purchaseOrderService.createOrder(testOrder);

            // Then
            assertNotNull(orderId);
            assertEquals(0, testOrder.getStatus()); // 草稿状态
            assertNotNull(testOrder.getOrderNo());
            verify(purchaseOrderMapper, times(1)).insert(any(PurchaseOrder.class));
        }
    }

    @Test
    @DisplayName("创建采购订单 - 自动生成订单号")
    void testCreateOrder_GenerateOrderNo() {
        // Given
        when(purchaseOrderMapper.insert(any(PurchaseOrder.class))).thenReturn(1);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            // When
            purchaseOrderService.createOrder(testOrder);

            // Then
            assertNotNull(testOrder.getOrderNo());
            assertTrue(testOrder.getOrderNo().startsWith("PO"));
        }
    }

    @Test
    @DisplayName("创建采购订单 - 已入库金额初始化为0")
    void testCreateOrder_InitializeReceivedAmount() {
        // Given
        when(purchaseOrderMapper.insert(any(PurchaseOrder.class))).thenReturn(1);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            // When
            purchaseOrderService.createOrder(testOrder);

            // Then
            assertEquals(BigDecimal.ZERO, testOrder.getReceivedAmount());
        }
    }
}