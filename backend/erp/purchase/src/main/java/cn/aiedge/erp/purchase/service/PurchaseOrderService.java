package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.entity.PurchaseContract;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.entity.PurchaseOrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 采购订单服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface PurchaseOrderService extends IService<PurchaseOrder> {

    /**
     * 创建采购订单
     * 
     * @param order 订单信息
     * @return 订单ID
     */
    Long createOrder(PurchaseOrder order);

    /**
     * 更新采购订单
     * 
     * @param order 订单信息
     */
    void updateOrder(PurchaseOrder order);

    /**
     * 删除采购订单
     * 
     * @param orderId 订单ID
     */
    void deleteOrder(Long orderId);

    /**
     * 提交审批
     * 
     * @param orderId 订单ID
     */
    void submitForApproval(Long orderId);

    /**
     * 审批通过
     * 
     * @param orderId 订单ID
     */
    void approve(Long orderId);

    /**
     * 审批拒绝
     * 
     * @param orderId 订单ID
     * @param reason 拒绝原因
     */
    void reject(Long orderId, String reason);

    /**
     * 取消订单
     * 
     * @param orderId 订单ID
     * @param reason 取消原因
     */
    void cancel(Long orderId, String reason);

    /**
     * 分页查询
     * 
     * @param page 分页参数
     * @param tenantId 租户ID
     * @param orderNo 订单号
     * @param supplierId 供应商ID
     * @param status 状态
     * @return 分页结果
     */
    Page<PurchaseOrder> pageOrders(Page<PurchaseOrder> page, Long tenantId,
                                   String orderNo, Long supplierId, Integer status);

    /**
     * 获取订单详情
     * 
     * @param orderId 订单ID
     * @return 订单详情
     */
    PurchaseOrder getOrderDetail(Long orderId);

    /**
     * 获取订单明细列表
     * 
     * @param orderId 订单ID
     * @return 明细列表
     */
    List<Object> getOrderItems(Long orderId);

    /**
     * 提交订单
     */
    PurchaseOrder submitOrder(Long orderId);

    /**
     * 审批订单
     */
    PurchaseOrder approveOrder(Long orderId, Long approverId, String comment);

    /**
     * 下达订单
     */
    PurchaseOrder issueOrder(Long orderId);

    /**
     * 从合同生成订单
     */
    PurchaseOrder generateOrderFromContract(PurchaseContract contract, List<PurchaseOrderItem> items);

    /**
     * 开始履行
     */
    PurchaseOrder startFulfillment(Long orderId);

    /**
     * 更新履行进度
     */
    PurchaseOrder updateFulfillmentProgress(Long orderId, BigDecimal receivedAmount, BigDecimal fulfillmentPercent);

    /**
     * 完成订单
     */
    PurchaseOrder completeOrder(Long orderId);

    /**
     * 供应商确认
     * 
     * @param orderId 订单ID
     */
    void confirmBySupplier(Long orderId);

    /**
     * 发货通知
     * 
     * @param orderId 订单ID
     * @param trackingNumber 物流跟踪号
     * @param estimatedArrivalTime 预计到达时间
     */
    void shipOrder(Long orderId, String trackingNumber, LocalDateTime estimatedArrivalTime);

    /**
     * 收货确认
     * 
     * @param orderId 订单ID
     * @param receivedQuantity 收货数量
     * @param qualityCheckResult 质量检查结果
     */
    void receiveOrder(Long orderId, BigDecimal receivedQuantity, String qualityCheckResult);

    /**
     * 发票提交
     * 
     * @param orderId 订单ID
     * @param invoiceNumber 发票号
     * @param invoiceAmount 发票金额
     * @param invoiceDate 发票日期
     */
    void submitInvoice(Long orderId, String invoiceNumber, BigDecimal invoiceAmount, LocalDate invoiceDate);

    /**
     * 待审批订单列表
     * 
     * @return 待审批订单列表
     */
    List<PurchaseOrder> getPendingApprovalOrders();

    /**
     * 导出采购订单
     */
    List<PurchaseOrder> exportOrders(Long tenantId, String orderNo, Long supplierId, Integer status);

    /**
     * 获取采购统计信息
     *
     * @param tenantId 租户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计信息
     */
    Map<String, Object> getPurchaseStatistics(Long tenantId, LocalDate startDate, LocalDate endDate);

    /**
     * 生成采购报表
     * 
     * @param tenantId 租户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 报表数据
     */
    List<Map<String, Object>> generatePurchaseReport(Long tenantId, LocalDate startDate, LocalDate endDate);
}