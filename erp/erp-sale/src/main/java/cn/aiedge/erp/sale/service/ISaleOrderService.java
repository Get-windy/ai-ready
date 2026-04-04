package cn.aiedge.erp.sale.service;

import cn.aiedge.erp.sale.dto.SaleOrderDTO;
import cn.aiedge.erp.sale.entity.SaleOrder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售订单服务接口
 */
public interface ISaleOrderService extends IService<SaleOrder> {

    /**
     * 分页查询订单
     */
    Page<SaleOrderDTO> pageOrders(Page<SaleOrder> page, Long tenantId, String orderNo, 
                                   Long customerId, Integer status, String startDate, String endDate);

    /**
     * 获取订单详情（含明细）
     */
    SaleOrderDTO getOrderDetail(Long id);

    /**
     * 创建订单
     */
    Long createOrder(SaleOrderDTO dto);

    /**
     * 更新订单
     */
    void updateOrder(SaleOrderDTO dto);

    /**
     * 删除订单
     */
    void deleteOrder(Long id);

    /**
     * 提交审批
     */
    void submitForApproval(Long id);

    /**
     * 审批通过
     */
    void approve(Long id, Long auditorId);

    /**
     * 审批拒绝
     */
    void reject(Long id, Long auditorId, String reason);

    /**
     * 取消订单
     */
    void cancelOrder(Long id, String reason);

    /**
     * 确认出库（扣减库存）
     */
    void confirmShipment(Long id, Long warehouseId);

    /**
     * 记录收款
     */
    void recordPayment(Long id, BigDecimal amount);

    /**
     * 获取待审批订单
     */
    List<SaleOrderDTO> getPendingOrders(Long tenantId);

    /**
     * 生成订单号
     */
    String generateOrderNo();

    /**
     * 计算订单金额
     */
    void calculateAmount(SaleOrderDTO dto);
}