package cn.aiedge.erp.sale.outbound.service;

import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import cn.aiedge.erp.sale.outbound.entity.SaleOutboundItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface SaleOutboundService extends IService<SaleOutbound> {

    SaleOutbound getByOutboundNo(String outboundNo);

    Page<SaleOutbound> pageList(String keyword, Long customerId, Long orderId, Long warehouseId, Integer status, int pageNum, int pageSize);

    List<SaleOutbound> listByCustomerId(Long customerId);

    List<SaleOutbound> listByOrderId(Long orderId);

    String generateOutboundNo();

    SaleOutbound createOutbound(SaleOutbound outbound, List<SaleOutboundItem> items);

    SaleOutbound createFromOrder(Long orderId);

    SaleOutbound updateOutbound(Long outboundId, SaleOutbound outbound, List<SaleOutboundItem> items);

    SaleOutbound submitForApproval(Long outboundId);

    SaleOutbound approve(Long outboundId, Long approverId, String note);

    SaleOutbound reject(Long outboundId, String reason);

    SaleOutbound startPicking(Long outboundId, Long pickerId);

    SaleOutboundItem pickItem(Long itemId, BigDecimal outboundQuantity, String batchNo);

    SaleOutbound completePicking(Long outboundId);

    SaleOutbound startPacking(Long outboundId, Long packerId);

    SaleOutboundItem packItem(Long itemId);

    SaleOutbound completePacking(Long outboundId);

    SaleOutbound ship(Long outboundId, Long shipperId, String trackingNumber, String logisticsCompany);

    SaleOutbound complete(Long outboundId);

    SaleOutbound cancel(Long outboundId, String reason);

    void calculateTotals(Long outboundId);

    List<SaleOutboundItem> getItems(Long outboundId);

    SaleOutboundItem addItem(Long outboundId, SaleOutboundItem item);

    SaleOutboundItem updateItem(Long itemId, SaleOutboundItem item);

    void removeItem(Long itemId);

    void updateStock(Long outboundId);
}