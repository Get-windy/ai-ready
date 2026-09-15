package cn.aiedge.erp.sale.salereturn.service;

import cn.aiedge.erp.sale.salereturn.dto.SaleReturnItemPageDTO;
import cn.aiedge.erp.sale.salereturn.entity.SaleReturn;
import cn.aiedge.erp.sale.salereturn.entity.SaleReturnItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SaleReturnService extends IService<SaleReturn> {

    SaleReturn getByReturnNo(String returnNo);

    SaleReturn getByIdWithItems(Long id);

    /**
     * 按单据分页查询（Tab1「按单据」）。
     *
     * <p>本轮新增参数（均为"为空不过滤"）：{@code returnNo}、{@code sourceOrder}、
     * {@code settleUnit}、{@code extText4}、{@code extText5}；{@code extNum1}/{@code extNum2}
     * 由 {@code Integer} 改为 {@code BigDecimal}（匹配列类型 DECIMAL(18,2)）；{@code auditTime}
     * 由"仅 ≥"改为"当日区间"（单日筛选语义）。</p>
     */
    Page<SaleReturn> pageList(int pageNum, int pageSize, String keyword, String customerName,
                              String handlerName, String deptName, String warehouseName,
                              String productName, String itemRemark, Integer status,
                              String generateType, String settleStatus, Integer printCount,
                              String startDate, String endDate, Long categoryId,
                              String creatorName, String auditorName, String submitBy,
                              String productLineAttr, String remark, String summary,
                              String deliveryMethod, BigDecimal extNum1, BigDecimal extNum2,
                              String extText1, String extText2, String extText3,
                              String extText4, String extText5,
                              String contactName, String contactPhone, String contactAddress,
                              String auditTime, String salesType,
                              String returnNo, String sourceOrder, String settleUnit);

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
     * 按明细分页查询退货申请单（Tab2「按明细」）。
     *
     * <p>返回强类型 {@link SaleReturnItemPageDTO}（原为 {@code Page<Map<String,Object>>}，字段名
     * 依赖 Map key 约定）。本轮新增参数（均为"为空不过滤"）：{@code customerName}、
     * {@code warehouseName}、{@code sourceOrder}、{@code handlerName}、{@code settleUnit}、
     * {@code extNum1}、{@code extNum2}、{@code extText3}~{@code extText5}。</p>
     */
    Page<SaleReturnItemPageDTO> pageDetail(
            String keyword, Long customerId, Long warehouseId, Integer status,
            String returnNo, String productName, Long handlerId, String handlerName, String deptName,
            String settleStatus, String salesType, String productLineAttr,
            String itemRemark, String startDate, String endDate,
            int pageNum, int pageSize,
            String creatorName, String auditorName, String remark,
            Boolean isGift, String auditTime, Long categoryId,
            String customerName, String warehouseName, String sourceOrder, String settleUnit,
            BigDecimal extNum1, BigDecimal extNum2,
            String extText3, String extText4, String extText5);

    /**
     * 批量审批退货申请单
     */
    int batchApprove(List<Long> ids, String note);

    /**
     * 收货进度回写（《物流退货收货》跟踪口径的唯一写入口）。
     * <p>由销售退货单收货/取消时调用：按商品汇总已收货数量，刷新申请明细与主表的
     * 已收数量 / 未收数量，并把全部收货的申请推进到「已完成(3)」。</p>
     *
     * @param returnApplyId     退货申请单 ID
     * @param receivedByProduct 商品 ID → 累计已收货数量（来自所有已审批退货单）
     */
    void writebackReceivedProgress(Long returnApplyId, Map<Long, BigDecimal> receivedByProduct);
}
