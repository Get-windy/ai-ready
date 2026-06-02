package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dto.*;

import java.util.List;

public interface MallOrderService {

    OrderDetailDTO createOrder(OrderCreateRequest request);

    PageResult<OrderListDTO> listOrders(int page, int size, String orderStatus);

    OrderDetailDTO getOrderDetail(Long id);

    void cancelOrder(Long id);

    void confirmOrder(Long id);

    void payOrder(Long id);

    List getPaymentMethods();

    void trackOrder(Long id);
}
