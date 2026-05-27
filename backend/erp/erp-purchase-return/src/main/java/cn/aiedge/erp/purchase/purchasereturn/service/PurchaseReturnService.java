package cn.aiedge.erp.purchase.purchasereturn.service;

import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturn;
import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturnItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PurchaseReturnService extends IService<PurchaseReturn> {

    PurchaseReturn getByReturnNo(String returnNo);

    Page<PurchaseReturn> pageList(String keyword, Long supplierId, Integer status, int pageNum, int pageSize);

    List<PurchaseReturn> listBySupplierId(Long supplierId);

    String generateReturnNo();

    PurchaseReturn createReturn(PurchaseReturn returnOrder, List<PurchaseReturnItem> items);

    PurchaseReturn submitForApproval(Long returnId);

    PurchaseReturn approve(Long returnId, Long approverId, String note);

    PurchaseReturn reject(Long returnId, String reason);

    PurchaseReturn complete(Long returnId);

    PurchaseReturn cancel(Long returnId, String reason);

    List<PurchaseReturnItem> getItems(Long returnId);

    PurchaseReturnItem addItem(Long returnId, PurchaseReturnItem item);

    PurchaseReturnItem updateItem(Long itemId, PurchaseReturnItem item);

    void removeItem(Long itemId);
}