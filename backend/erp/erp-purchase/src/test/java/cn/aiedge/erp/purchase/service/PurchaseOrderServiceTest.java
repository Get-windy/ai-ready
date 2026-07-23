package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.dto.PurchaseOrderDTO;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.entity.PurchaseOrderLogistics;
import cn.aiedge.erp.purchase.entity.PurchaseOrderSettlement;
import cn.aiedge.erp.purchase.mapper.*;
import cn.aiedge.erp.purchase.service.impl.PurchaseOrderServiceImpl;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

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
    private PurchaseOrderMapper orderMapper;
    @Mock
    private PurchaseOrderItemMapper orderItemMapper;
    @Mock
    private PurchaseOrderPartnerSnapshotMapper partnerSnapshotMapper;
    @Mock
    private PurchaseOrderSettlementMapper settlementMapper;
    @Mock
    private PurchaseOrderLogisticsMapper logisticsMapper;
    @Mock
    private PurchaseOrderDepositMapper depositMapper;
    @Mock
    private PurchaseOrderAuditTrailMapper auditTrailMapper;
    @Mock
    private PurchaseOrderExtInfoMapper extInfoMapper;

    @Spy
    @InjectMocks
    private PurchaseOrderServiceImpl purchaseOrderService;

    private PurchaseOrder testOrder;

    @BeforeEach
    void setUp() {
        // MyBatis-Plus 3.5.10 的 CrudRepository.baseMapper 走字段注入，
        // @InjectMocks 构造注入后 baseMapper 为 null，这里显式注入
        ReflectionTestUtils.setField(purchaseOrderService, "baseMapper", orderMapper);

        testOrder = new PurchaseOrder();
        testOrder.setTenantId(1L);
        testOrder.setSupplierId(100L);
        testOrder.setBillAmount(new BigDecimal("11300.00"));
    }

    /** 让 MyBatis-Plus 的 save() 在纯单测环境下直接成功并回填主键 */
    private void stubSaveAssignsId(Long id) {
        doAnswer(invocation -> {
            PurchaseOrder o = invocation.getArgument(0);
            o.setId(id);
            return true;
        }).when(purchaseOrderService).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("创建采购订单 - 成功")
    void testCreateOrder_Success() {
        stubSaveAssignsId(123L);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            PurchaseOrderDTO dto = new PurchaseOrderDTO();
            dto.setOrder(testOrder);
            Long orderId = purchaseOrderService.createOrder(dto);

            assertEquals(123L, orderId);
            assertEquals(0, testOrder.getStatus()); // 草稿状态
            assertNotNull(testOrder.getOrderNo());
            assertEquals(1L, testOrder.getCreateBy());
            verify(purchaseOrderService, times(1)).save(any(PurchaseOrder.class));
        }
    }

    @Test
    @DisplayName("创建采购订单 - 自动生成订单号（CG+日期前缀）")
    void testCreateOrder_GenerateOrderNo() {
        stubSaveAssignsId(124L);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            PurchaseOrderDTO dto = new PurchaseOrderDTO();
            dto.setOrder(testOrder);
            purchaseOrderService.createOrder(dto);

            assertNotNull(testOrder.getOrderNo());
            assertTrue(testOrder.getOrderNo().startsWith("CG"),
                    "订单号应以CG前缀生成，实际: " + testOrder.getOrderNo());
        }
    }

    @Test
    @DisplayName("创建采购订单 - 无明细时金额字段初始化为0")
    void testCreateOrder_InitializeAmounts() {
        stubSaveAssignsId(125L);
        testOrder.setProductAmount(null);
        testOrder.setDiscountAmount(null);
        testOrder.setBillAmount(null);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            PurchaseOrderDTO dto = new PurchaseOrderDTO();
            dto.setOrder(testOrder);
            purchaseOrderService.createOrder(dto);

            assertEquals(BigDecimal.ZERO, testOrder.getProductAmount());
            assertEquals(BigDecimal.ZERO, testOrder.getDiscountAmount());
            assertEquals(BigDecimal.ZERO, testOrder.getBillAmount());
        }
    }

    @Test
    @DisplayName("创建采购订单 - 子表回填订单ID并落库")
    void testCreateOrder_SubTablesBackfillOrderId() {
        stubSaveAssignsId(126L);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            PurchaseOrderDTO dto = new PurchaseOrderDTO();
            dto.setOrder(testOrder);
            PurchaseOrderSettlement settlement = new PurchaseOrderSettlement();
            dto.setSettlement(settlement);
            PurchaseOrderLogistics logistics = new PurchaseOrderLogistics();
            dto.setLogisticsList(List.of(logistics));

            purchaseOrderService.createOrder(dto);

            assertEquals(126L, settlement.getOrderId());
            assertEquals(126L, logistics.getOrderId());
            verify(settlementMapper, times(1)).insert(settlement);
            verify(logisticsMapper, times(1)).insert(logistics);
        }
    }

    @Test
    @DisplayName("创建采购订单 - 主表为空时抛业务异常")
    void testCreateOrder_NullOrderRejected() {
        PurchaseOrderDTO dto = new PurchaseOrderDTO();
        dto.setOrder(null);
        assertThrows(Exception.class, () -> purchaseOrderService.createOrder(dto));
    }
}
