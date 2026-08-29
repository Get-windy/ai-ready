package cn.aiedge.erp.purchase.purchasereturn.service;

import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturn;
import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturnItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface PurchaseReturnService extends IService<PurchaseReturn> {

    PurchaseReturn getByReturnNo(String returnNo);

    Page<PurchaseReturn> pageList(String keyword, Long supplierId, Long orderId, Long warehouseId, Integer status, Integer settleStatus, int pageNum, int pageSize);

    List<PurchaseReturn> exportList(String keyword, Long supplierId, Long orderId, Long warehouseId, Integer status);

    List<PurchaseReturn> listBySupplierId(Long supplierId);

    List<PurchaseReturn> listByOrderId(Long orderId);

    String generateReturnNo();

    void batchPrint(List<Long> ids, String template);

    PurchaseReturn createReturn(PurchaseReturn returnOrder, List<PurchaseReturnItem> items);

    PurchaseReturn createFromOrder(Long orderId);

    PurchaseReturn updateReturn(Long returnId, PurchaseReturn returnOrder, List<PurchaseReturnItem> items);

    PurchaseReturn submitForApproval(Long returnId);

    PurchaseReturn approve(Long returnId, Long approverId, String note);

    PurchaseReturn reject(Long returnId, String reason);

    PurchaseReturn complete(Long returnId);

    PurchaseReturn cancel(Long returnId, String reason);

    void calculateTotals(Long returnId);

    List<PurchaseReturnItem> getItems(Long returnId);

    PurchaseReturnItem addItem(Long returnId, PurchaseReturnItem item);

    PurchaseReturnItem updateItem(Long itemId, PurchaseReturnItem item);

    void removeItem(Long itemId);

    void updateStock(Long returnId);
}
