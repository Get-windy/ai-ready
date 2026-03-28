package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

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
}