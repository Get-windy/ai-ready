package cn.aiedge.order.service;

import cn.aiedge.order.dto.OrderDTO;
import cn.aiedge.order.entity.Order;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单服务接口
 */
public interface IOrderService extends IService<Order> {

    Page<OrderDTO> pageOrders(Page<Order> page, Long tenantId, String orderNo, String customerName,
                               Integer orderType, Integer status, Long saleId,
                               java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    OrderDTO getOrderDetail(Long id);

    Long createOrder(OrderDTO dto);

    void updateOrder(OrderDTO dto);

    void deleteOrder(Long id);

    void updateStatus(Long id, Integer status);

    void auditOrder(Long id, Integer status, String auditRemark);

    void addReceivedAmount(Long id, BigDecimal amount);

    String generateOrderNo();
}
