package cn.aiedge.erp.sale.returnDoc.service;

import cn.aiedge.erp.sale.returnDoc.entity.SaleReturnDoc;
import cn.aiedge.erp.sale.returnDoc.entity.SaleReturnDocItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface SaleReturnDocService extends IService<SaleReturnDoc> {

    SaleReturnDoc getByReturnDocNo(String returnDocNo);

    SaleReturnDoc getByIdWithItems(Long id);

    Page<SaleReturnDoc> pageList(int pageNum, int pageSize, String keyword, String customerName,
                                  String handlerName, String deptName, String warehouseName,
                                  String productName, String itemRemark, Integer status,
                                  String generateType, String settleStatus, Integer printCount,
                                  String startDate, String endDate, Long categoryId,
                                  String creatorName, String auditorName, String submitBy,
                                  String productLineAttr, String remark, String summary,
                                  String deliveryMethod, Integer extNum1, Integer extNum2,
                                  String extText1, String extText2, String extText3,
                                  String contactName, String contactPhone, String contactAddress,
                                  String auditTime, String salesType, String receiverName,
                                  String logisticsCompany, String waybillNo, String region,
                                  Boolean showRedFlush);

    List<SaleReturnDoc> exportList(String keyword, String customerName, Integer status);

    List<SaleReturnDoc> listByCustomerId(Long customerId);

    String generateReturnDocNo();

    SaleReturnDoc createReturnDoc(SaleReturnDoc returnDoc);

    SaleReturnDoc updateReturnDoc(SaleReturnDoc returnDoc);

    SaleReturnDoc submitForApproval(Long returnDocId);

    SaleReturnDoc approve(Long returnDocId, String note);

    SaleReturnDoc reject(Long returnDocId, String reason);

    SaleReturnDoc complete(Long returnDocId);

    SaleReturnDoc cancel(Long returnDocId, String reason);

    List<SaleReturnDocItem> getItems(Long returnDocId);

    SaleReturnDocItem addItem(Long returnDocId, SaleReturnDocItem item);

    SaleReturnDocItem updateItem(Long itemId, SaleReturnDocItem item);

    void removeItem(Long itemId);

    /**
     * 按明细分页查询退货单
     */
    Page<Map<String, Object>> pageDetail(
            String keyword, Long customerId, Long warehouseId, Integer status,
            String returnDocNo, String productName, Long handlerId, String deptName,
            String settleStatus, String salesType, String productLineAttr,
            String itemRemark, String startDate, String endDate,
            int pageNum, int pageSize,
            String creatorName, String auditorName, String remark,
            Boolean isGift, String auditTime);

    /**
     * 批量审批退货单
     */
    int batchApprove(List<Long> ids, String note);

    /**
     * 计算退货单总金额和总数量
     */
    void calculateTotals(Long returnDocId);
}
