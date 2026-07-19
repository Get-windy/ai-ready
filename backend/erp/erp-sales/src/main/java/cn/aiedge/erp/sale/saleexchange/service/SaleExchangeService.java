package cn.aiedge.erp.sale.saleexchange.service;

import cn.aiedge.erp.sale.saleexchange.entity.ExchangeApprovalRecord;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchange;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchangeItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface SaleExchangeService extends IService<SaleExchange> {

    Page<SaleExchange> pageList(String keyword, Long customerId, Integer status, Integer exchangeType,
                                String startDate, String endDate, int pageNum, int pageSize);

    /**
     * 扩展分页查询（支持更多条件）
     */
    Page<SaleExchange> pageListExtended(Map<String, Object> params, int pageNum, int pageSize);

    List<SaleExchange> exportList(String keyword, Long customerId, Integer status, Integer exchangeType,
                                  String startDate, String endDate);

    String generateExchangeNo();

    SaleExchange createExchange(SaleExchange exchange, List<SaleExchangeItem> items);

    SaleExchange updateExchange(Long id, SaleExchange exchange, List<SaleExchangeItem> items);

    SaleExchange submitForApproval(Long id);

    SaleExchange approve(Long id, Long approverId, String approvedByName, String remark);

    /**
     * 批量审核
     */
    int batchApprove(List<Long> ids, Long approverId, String approvedByName);

    SaleExchange reject(Long id, String remark);

    SaleExchange cancel(Long id, String reason);

    SaleExchange complete(Long id);

    /**
     * 打印（打印次数+1）
     */
    void print(Long id);

    /**
     * 批量打印
     */
    void batchPrint(List<Long> ids);

    List<SaleExchangeItem> getItems(Long exchangeId);

    /**
     * 按仓库类型获取明细
     */
    List<SaleExchangeItem> getItemsByWarehouseType(Long exchangeId, Integer warehouseType);

    List<ExchangeApprovalRecord> getApprovalRecords(Long exchangeId);

    Map<String, Object> getTracking(Long exchangeId);
}
