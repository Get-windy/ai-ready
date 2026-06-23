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

    List<SaleExchange> exportList(String keyword, Long customerId, Integer status, Integer exchangeType,
                                  String startDate, String endDate);

    String generateExchangeNo();

    SaleExchange createExchange(SaleExchange exchange, List<SaleExchangeItem> items);

    SaleExchange updateExchange(Long id, SaleExchange exchange, List<SaleExchangeItem> items);

    SaleExchange submitForApproval(Long id);

    SaleExchange approve(Long id, Long approverId, String approvedByName, String remark);

    SaleExchange reject(Long id, String remark);

    SaleExchange cancel(Long id, String reason);

    SaleExchange complete(Long id);

    List<SaleExchangeItem> getItems(Long exchangeId);

    List<ExchangeApprovalRecord> getApprovalRecords(Long exchangeId);

    Map<String, Object> getTracking(Long exchangeId);
}
