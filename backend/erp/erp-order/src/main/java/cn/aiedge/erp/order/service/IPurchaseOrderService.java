package cn.aiedge.erp.order.service;

import cn.aiedge.erp.order.dto.PurchaseOrderCreateDTO;
import cn.aiedge.erp.order.dto.PurchaseOrderApproveDTO;
import cn.aiedge.erp.order.dto.PurchaseOrderStatisticsDTO;
import cn.aiedge.erp.order.entity.PurchaseOrder;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购订单服务接口
 */
public interface IPurchaseOrderService extends IService<PurchaseOrder> {

    /**
     * 创建采购订单
     */
    PurchaseOrder createPurchaseOrder(PurchaseOrderCreateDTO createDTO);

    /**
     * 更新采购订单
     */
    PurchaseOrder updatePurchaseOrder(Long id, PurchaseOrderCreateDTO updateDTO);

    /**
     * 获取采购订单
     */
    PurchaseOrder getPurchaseOrderById(Long id);

    /**
     * 根据状态获取采购订单列表
     */
    List<PurchaseOrder> getPurchaseOrdersByStatus(String status);

    /**
     * 分页查询采购订单
     */
    IPage<PurchaseOrder> getPurchaseOrderPage(Integer page, Integer size, String status, Long supplierId);

    /**
     * 提交采购订单审批
     */
    PurchaseOrder submitForApproval(Long id);

    /**
     * 审批采购订单
     */
    PurchaseOrder approvePurchaseOrder(Long id, PurchaseOrderApproveDTO approveDTO);

    /**
     * 供应商确认采购订单
     */
    PurchaseOrder confirmSupplier(Long id);

    /**
     * 标记为已发货
     */
    PurchaseOrder markAsShipped(Long id);

    /**
     * 标记为已收货
     */
    PurchaseOrder markAsReceived(Long id);

    /**
     * 标记为已完成
     */
    PurchaseOrder markAsCompleted(Long id);

    /**
     * 取消采购订单
     */
    void cancelPurchaseOrder(Long id, String reason);

    /**
     * 获取采购订单统计
     */
    PurchaseOrderStatisticsDTO getPurchaseOrderStatistics(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 获取临期付款订单
     */
    List<PurchaseOrder> getDuePaymentOrders(int daysBeforeDue);

    /**
     * 根据供应商ID获取订单
     */
    List<PurchaseOrder> getOrdersBySupplier(Long supplierId);

    /**
     * 根据采购员ID获取订单
     */
    List<PurchaseOrder> getOrdersByBuyer(Long buyerId);

    // 以下是为了匹配Controller接口而添加的方法
    
    /**
     * 分页查询采购订单（Controller兼容方法）
     */
    Page<PurchaseOrder> pagePurchaseOrders(Page<PurchaseOrder> page, Long tenantId, String orderNo, String supplierName,
                                           Integer purchaseType, Integer status, Long buyerId,
                                           LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 获取采购订单详情（Controller兼容方法）
     */
    PurchaseOrder getPurchaseOrderDetail(Long id);

    /**
     * 创建采购订单（Controller兼容方法）
     */
    Long createPurchaseOrder(PurchaseOrderCreateDTO dto);

    /**
     * 更新采购订单（Controller兼容方法）
     */
    void updatePurchaseOrder(PurchaseOrderCreateDTO dto);

    /**
     * 删除采购订单（Controller兼容方法）
     */
    void deletePurchaseOrder(Long id);

    /**
     * 提交采购订单审批（Controller兼容方法）
     */
    void submitForApproval(Long orderId, String remark);

    /**
     * 审批采购订单（Controller兼容方法）
     */
    void approvePurchaseOrder(Long orderId, PurchaseOrderApproveDTO approveDTO);

    /**
     * 拒绝采购订单（Controller兼容方法）
     */
    void rejectPurchaseOrder(Long orderId, String rejectReason);

    /**
     * 获取待审批采购订单列表（Controller兼容方法）
     */
    List<PurchaseOrder> getPendingApprovalOrders(Long tenantId, Long approverId);

    /**
     * 供应商确认采购订单（Controller兼容方法）
     */
    void confirmBySupplier(Long orderId, String confirmationNo, String remark);

    /**
     * 发货通知（Controller兼容方法）
     */
    void shipOrder(Long orderId, String trackingNo, String logisticsCompany, LocalDateTime estimatedArrivalDate);

    /**
     * 收货确认（Controller兼容方法）
     */
    void receiveGoods(Long orderId, Integer receivedQuantity, String qualityCheckResult, String remark);

    /**
     * 发票提交（Controller兼容方法）
     */
    void submitInvoice(Long orderId, String invoiceNo, BigDecimal invoiceAmount, LocalDateTime invoiceDate);

    /**
     * 更新采购订单状态（Controller兼容方法）
     */
    void updatePurchaseOrderStatus(Long orderId, Integer status);

    /**
     * 获取采购统计信息（Controller兼容方法）
     */
    PurchaseOrderStatisticsDTO getPurchaseStatistics(Long tenantId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 生成采购报表（Controller兼容方法）
     */
    byte[] generatePurchaseReport(Long tenantId, LocalDateTime startDate, LocalDateTime endDate, String format);

    /**
     * 导出采购数据（Controller兼容方法）
     */
    byte[] exportPurchaseData(Long tenantId, LocalDateTime startDate, LocalDateTime endDate, String format);
}