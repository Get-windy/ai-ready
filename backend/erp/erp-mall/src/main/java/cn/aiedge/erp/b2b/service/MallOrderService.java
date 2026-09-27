package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dto.*;

import java.util.List;
import java.util.Map;

public interface MallOrderService {

    /** 创建订单 */
    OrderDetailDTO createOrder(OrderCreateRequest request);

    /** 分页查询订单列表 */
    PageResult<OrderListDTO> listOrders(int page, int size, String orderStatus);

    /** 获取订单详情 */
    OrderDetailDTO getOrderDetail(Long id);

    /** 取消订单 */
    void cancelOrder(Long id);

    /** 确认收货 */
    void confirmOrder(Long id);

    // ⚠️ 2026-09-23 移除了三个「买家端」方法：payOrder / approveOrder / rejectOrder。
    //   理由（详见 TRADE_MODULE_AUDIT_20260923.md P0-5、P2-5）：
    //     · payOrder 只把订单置为已付（receivedAmount=总额），**不产生任何支付记录** ⇒
    //       买家可凭一个 POST 把自己的订单变成已付款；真正的支付链路应走
    //       core-payment 的 createPayment + 渠道回调驱动状态。
    //     · approveOrder / rejectOrder 是**审核动作**，此处暴露给「已登录的买家」，
    //       等于买家可自审通过（绕过订单审核）。管理端已有带权限码的等价实现
    //       {@link MallAdminService#approveOrder} / {@link MallAdminService#rejectOrder}。
    //     · 三者在两个前端应用（pc-admin / mobile-mall）中**零调用方**。
    //   待 C 端支付与审核链路真正建设时，按上面的正确路径重新接入。

    /**
     * F-05「我的」订单五宫格 / 订单页顶部 Tab 的**状态计数**。
     *
     * <p>口径与 {@link #listOrders} **完全一致**（同一身份、同一订单来源），
     * 否则会出现"列表里有 3 单待付款、角标却写 2"这类对不上的情况。</p>
     *
     * @return key：pendingPayment 待付款 / pendingShip 待发货 / pendingReceive 待收货 /
     *         completed 已完成 / afterSales 售后（暂恒 0，见实现注释）
     */
    Map<String, Integer> orderStatusCounts();

    /**
     * F-06 订单物流信息。
     *
     * <p>返回该单的**发货信息**（物流公司 / 运单号 / 配送状态）。
     * 真实轨迹需要对接承运商（快递100 之类），本系统暂无数据源，
     * 故 {@code traces} 恒为空数组并附带说明 —— **不编造轨迹节点**。</p>
     */
    Map<String, Object> orderLogistics(Long id);

    /** 获取支付方式列表 */
    List<Map<String, Object>> getPaymentMethods();
}
