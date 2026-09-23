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

    /** 获取支付方式列表 */
    List<Map<String, Object>> getPaymentMethods();
}
