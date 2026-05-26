package cn.aiedge.erp.sale.return.service;

import cn.aiedge.erp.sale.return.entity.SaleReturn;
import cn.aiedge.erp.sale.return.entity.SaleReturnItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface SaleReturnService extends IService<SaleReturn> {

    SaleReturn getByReturnNo(String returnNo);

    Page<SaleReturn> pageList(String keyword, Long customerId, Long orderId, Integer status, Integer returnType, int pageNum, int pageSize);

    List<SaleReturn> listByCustomerId(Long customerId);

    List<SaleReturn> listByOrderId(Long orderId);

    String generateReturnNo();

    SaleReturn createReturn(SaleReturn returnOrder, List<SaleReturnItem> items);

    SaleReturn createFromOrder(Long orderId);

    SaleReturn updateReturn(Long returnId, SaleReturn returnOrder, List<SaleReturnItem> items);

    SaleReturn submitForApproval(Long returnId);

    SaleReturn approve(Long returnId, Long approverId, String note);

    SaleReturn reject(Long returnId, Long rejecterId, String reason);

    SaleReturn receive(Long returnId, Long receiverId);

    SaleReturnItem receiveItem(Long itemId, BigDecimal acceptedQuantity, BigDecimal rejectedQuantity, String qualityNote);

    SaleReturn confirmWarehouse(Long returnId, Long confirmerId);

    SaleReturn processRefund(Long returnId, String refundMethod, String refundAccount, String refundNote);

    SaleReturn completeRefund(Long returnId);

    SaleReturn complete(Long returnId);

    SaleReturn cancel(Long returnId, String reason);

    void calculateTotals(Long returnId);

    List<SaleReturnItem> getItems(Long returnId);

    SaleReturnItem addItem(Long returnId, SaleReturnItem item);

    SaleReturnItem updateItem(Long itemId, SaleReturnItem item);

    void removeItem(Long itemId);

    BigDecimal calculateRefundAmount(Long returnId);
}