package cn.aiedge.erp.sale.salereturn.service;

import cn.aiedge.erp.sale.salereturn.entity.SaleReturn;
import cn.aiedge.erp.sale.salereturn.entity.SaleReturnItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface SaleReturnService extends IService<SaleReturn> {

    SaleReturn getByReturnNo(String returnNo);

    Page<SaleReturn> pageList(String keyword, Long customerId, Integer status, int pageNum, int pageSize);

    List<SaleReturn> exportList(String keyword, Long customerId, Integer status);

    List<SaleReturn> listByCustomerId(Long customerId);

    String generateReturnNo();

    SaleReturn createReturn(SaleReturn returnOrder, List<SaleReturnItem> items);

    SaleReturn submitForApproval(Long returnId);

    SaleReturn approve(Long returnId, Long approverId, String note);

    SaleReturn reject(Long returnId, String reason);

    SaleReturn complete(Long returnId);

    SaleReturn cancel(Long returnId, String reason);

    List<SaleReturnItem> getItems(Long returnId);

    SaleReturnItem addItem(Long returnId, SaleReturnItem item);

    SaleReturnItem updateItem(Long itemId, SaleReturnItem item);

    void removeItem(Long itemId);
}
