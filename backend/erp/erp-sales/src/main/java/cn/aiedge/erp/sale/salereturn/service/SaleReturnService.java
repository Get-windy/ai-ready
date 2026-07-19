package cn.aiedge.erp.sale.salereturn.service;

import cn.aiedge.erp.sale.salereturn.entity.SaleReturn;
import cn.aiedge.erp.sale.salereturn.entity.SaleReturnItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface SaleReturnService extends IService<SaleReturn> {

    SaleReturn getByReturnNo(String returnNo);

    SaleReturn getByIdWithItems(Long id);

    Page<SaleReturn> pageList(int pageNum, int pageSize, String keyword, String customerName,
                              String handlerName, String deptName, String warehouseName,
                              String productName, String itemRemark, Integer status,
                              String generateType, String settleStatus, Integer printCount,
                              String startDate, String endDate, Long categoryId,
                              String creatorName, String auditorName, String submitBy,
                              String productLineAttr, String remark, String summary,
                              String deliveryMethod, Integer extNum1, Integer extNum2,
                              String extText1, String extText2, String extText3,
                              String contactName, String contactPhone, String contactAddress,
                              String auditTime, String salesType);

    List<SaleReturn> exportList(String keyword, String customerName, Integer status);

    List<SaleReturn> listByCustomerId(Long customerId);

    String generateReturnNo();

    SaleReturn createReturn(SaleReturn returnOrder);

    SaleReturn updateReturn(SaleReturn returnOrder);

    SaleReturn submitForApproval(Long returnId);

    SaleReturn approve(Long returnId, String note);

    SaleReturn reject(Long returnId, String reason);

    SaleReturn complete(Long returnId);

    SaleReturn cancel(Long returnId, String reason);

    List<SaleReturnItem> getItems(Long returnId);

    SaleReturnItem addItem(Long returnId, SaleReturnItem item);

    SaleReturnItem updateItem(Long itemId, SaleReturnItem item);

    void removeItem(Long itemId);

    /**
     * 按明细分页查询退货申请单
     */
    Page<Map<String, Object>> pageDetail(
            String keyword, Long customerId, Long warehouseId, Integer status,
            String returnNo, String productName, Long handlerId, String deptName,
            String settleStatus, String salesType, String productLineAttr,
            String itemRemark, String startDate, String endDate,
            int pageNum, int pageSize,
            String creatorName, String auditorName, String remark,
            Boolean isGift, String auditTime);

    /**
     * 批量审批退货申请单
     */
    int batchApprove(List<Long> ids, String note);
}
