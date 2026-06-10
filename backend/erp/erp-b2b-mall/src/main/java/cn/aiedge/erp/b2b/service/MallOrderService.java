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

    /** 支付订单 */
    void payOrder(Long id);

    /** 审核通过（管理端） */
    void approveOrder(Long id);

    /** 审核驳回（管理端） */
    void rejectOrder(Long id, String reason);

    /** 获取支付方式列表 */
    List<Map<String, Object>> getPaymentMethods();
}
