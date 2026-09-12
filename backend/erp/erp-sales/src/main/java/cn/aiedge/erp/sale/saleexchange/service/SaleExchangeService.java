package cn.aiedge.erp.sale.saleexchange.service;

import cn.aiedge.erp.sale.saleexchange.dto.SaleExchangeQuery;
import cn.aiedge.erp.sale.saleexchange.entity.ExchangeApprovalRecord;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchange;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchangeItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface SaleExchangeService extends IService<SaleExchange> {

    /**
     * 分页查询（按单据，支持开发文档「查询条件」20 项）
     */
    Page<SaleExchange> pageListExtended(SaleExchangeQuery query);

    /**
     * 导出列表（同查询条件，不分页）
     */
    List<SaleExchange> exportList(SaleExchangeQuery query);

    /**
     * 生成下一个单据编号（号段 XSHHD-yyyyMMdd-NNNN）
     */
    String generateExchangeNo();

    /**
     * 创建换货单（主表 + 明细全字段落库）
     */
    SaleExchange createExchange(SaleExchange exchange);

    /**
     * 更新换货单（仅草稿可改，明细整体替换）
     */
    SaleExchange updateExchange(Long id, SaleExchange exchange);

    /**
     * 提交审批：0 草稿 → 1 待审核
     */
    SaleExchange submitForApproval(Long id);

    /**
     * 审批通过：1 待审核 → 2 已审核，并真实过账库存（换入 +、换出 −）
     */
    SaleExchange approve(Long id, Long approverId, String approvedByName, String remark);

    /**
     * 批量审核
     */
    int batchApprove(List<Long> ids, Long approverId, String approvedByName);

    /**
     * 审批拒绝：1 待审核 → 5 已拒绝
     */
    SaleExchange reject(Long id, String remark);

    /**
     * 取消：已过账单据先回滚库存，再置为 6 已取消
     */
    SaleExchange cancel(Long id, String reason);

    /**
     * 完成：2 已审核 → 4 已完成
     */
    SaleExchange complete(Long id);

    /**
     * 删除（逻辑删除主表并级联删除明细）
     */
    boolean deleteExchange(Long id);

    /**
     * 打印（打印次数+1）
     */
    void print(Long id);

    /**
     * 批量打印
     */
    void batchPrint(List<Long> ids);

    /**
     * 导出 Excel（真实 xlsx 字节流）
     */
    byte[] exportExcel(SaleExchangeQuery query);

    List<SaleExchangeItem> getItems(Long exchangeId);

    /**
     * 按仓库类型获取明细（1=换入, 2=换出）
     */
    List<SaleExchangeItem> getItemsByWarehouseType(Long exchangeId, Integer warehouseType);

    List<ExchangeApprovalRecord> getApprovalRecords(Long exchangeId);

    Map<String, Object> getTracking(Long exchangeId);
}
