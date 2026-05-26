package cn.aiedge.erp.order.service;

import cn.aiedge.erp.order.dto.PurchaseOrderCreateDTO;
import cn.aiedge.erp.order.dto.PurchaseOrderApproveDTO;
import cn.aiedge.erp.order.entity.PurchaseOrder;
import cn.aiedge.erp.order.service.impl.PurchaseOrderServiceImpl;
import cn.aiedge.erp.order.mapper.PurchaseOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderMapper purchaseOrderMapper;

    @InjectMocks
    private PurchaseOrderServiceImpl purchaseOrderService;

    private PurchaseOrderCreateDTO testCreateDTO;
    private PurchaseOrder testOrder;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testCreateDTO = new PurchaseOrderCreateDTO();
        testCreateDTO.setOrderNumber("PO-20240505001");
        testCreateDTO.setSupplierId(1L);
        testCreateDTO.setSupplierName("测试供应商");
        testCreateDTO.setBuyerId(101L);
        testCreateDTO.setPurchaseType("STANDARD");
        testCreateDTO.setTotalAmount(new BigDecimal("10000.00"));
        testCreateDTO.setCurrency("CNY");
        testCreateDTO.setOrderDate(LocalDateTime.now());
        testCreateDTO.setExpectedDeliveryDate(LocalDateTime.now().plusDays(7));

        testOrder = new PurchaseOrder();
        testOrder.setId(1L);
        testOrder.setOrderNumber("PO-20240505001");
        testOrder.setSupplierId(1L);
        testOrder.setSupplierName("测试供应商");
        testOrder.setBuyerId(101L);
        testOrder.setPurchaseType("STANDARD");
        testOrder.setTotalAmount(new BigDecimal("10000.00"));
        testOrder.setCurrency("CNY");
        testOrder.setStatus("DRAFT");
        testOrder.setDeleted(false);
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createPurchaseOrder_ShouldReturnCreatedOrder() {
        // 准备
        when(purchaseOrderMapper.insert(any(PurchaseOrder.class))).thenReturn(1);
        
        // 执行
        PurchaseOrder result = purchaseOrderService.createPurchaseOrder(testCreateDTO);
        
        // 验证
        assertNotNull(result);
        assertEquals("PO-20240505001", result.getOrderNumber());
        assertEquals("DRAFT", result.getStatus());
        verify(purchaseOrderMapper, times(1)).insert(any(PurchaseOrder.class));
    }

    @Test
    void getPurchaseOrderById_WhenExists_ShouldReturnOrder() {
        // 准备
        when(purchaseOrderMapper.selectById(1L)).thenReturn(testOrder);
        
        // 执行
        PurchaseOrder result = purchaseOrderService.getPurchaseOrderById(1L);
        
        // 验证
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PO-20240505001", result.getOrderNumber());
    }

    @Test
    void getPurchaseOrderById_WhenNotExists_ShouldReturnNull() {
        // 准备
        when(purchaseOrderMapper.selectById(999L)).thenReturn(null);
        
        // 执行
        PurchaseOrder result = purchaseOrderService.getPurchaseOrderById(999L);
        
        // 验证
        assertNull(result);
    }

    @Test
    void submitForApproval_WhenDraftOrder_ShouldUpdateStatus() {
        // 准备
        when(purchaseOrderMapper.selectById(1L)).thenReturn(testOrder);
        
        // 执行
        PurchaseOrder result = purchaseOrderService.submitForApproval(1L);
        
        // 验证
        assertNotNull(result);
        assertEquals("SUBMITTED", result.getStatus());
        assertEquals("PENDING_APPROVAL", result.getApprovalStatus());
        verify(purchaseOrderMapper, times(1)).updateById(any(PurchaseOrder.class));
    }

    @Test
    void approvePurchaseOrder_WhenSubmittedOrder_ShouldUpdateStatus() {
        // 准备
        testOrder.setStatus("SUBMITTED");
        testOrder.setApprovalStatus("PENDING_APPROVAL");
        when(purchaseOrderMapper.selectById(1L)).thenReturn(testOrder);
        
        PurchaseOrderApproveDTO approveDTO = new PurchaseOrderApproveDTO();
        approveDTO.setResult("APPROVED");
        approveDTO.setApproverId(201L);
        approveDTO.setApprovalTime(LocalDateTime.now());
        
        // 执行
        PurchaseOrder result = purchaseOrderService.approvePurchaseOrder(1L, approveDTO);
        
        // 验证
        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        assertEquals("APPROVED", result.getApprovalStatus());
        verify(purchaseOrderMapper, times(1)).updateById(any(PurchaseOrder.class));
    }

    @Test
    void cancelPurchaseOrder_WhenDraftOrder_ShouldCancelSuccessfully() {
        // 准备
        testOrder.setStatus("DRAFT");
        when(purchaseOrderMapper.selectById(1L)).thenReturn(testOrder);
        
        // 执行
        purchaseOrderService.cancelPurchaseOrder(1L, "测试取消原因");
        
        // 验证
        verify(purchaseOrderMapper, times(1)).updateById(any(PurchaseOrder.class));
    }

    @Test
    void deletePurchaseOrder_ShouldMarkAsDeleted() {
        // 准备
        when(purchaseOrderMapper.selectById(1L)).thenReturn(testOrder);
        
        // 执行
        purchaseOrderService.deletePurchaseOrder(1L);
        
        // 验证
        assertTrue(testOrder.getDeleted());
        verify(purchaseOrderMapper, times(1)).updateById(any(PurchaseOrder.class));
    }
}