package cn.aiedge.erp.purchase.purchaseexchange.service;

import cn.aiedge.erp.purchase.purchaseexchange.entity.ExchangeApprovalRecord;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchange;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchangeItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface PurchaseExchangeService extends IService<PurchaseExchange> {

    Page<PurchaseExchange> pageList(String keyword, Long supplierId, Integer status, Integer exchangeType,
                                    String startDate, String endDate, int pageNum, int pageSize);

    List<PurchaseExchange> exportList(String keyword, Long supplierId, Integer status, Integer exchangeType,
                                      String startDate, String endDate);

    String generateExchangeNo();

    PurchaseExchange createExchange(PurchaseExchange exchange, List<PurchaseExchangeItem> items);

    PurchaseExchange updateExchange(Long id, PurchaseExchange exchange, List<PurchaseExchangeItem> items);

    PurchaseExchange submitForApproval(Long id);

    PurchaseExchange approve(Long id, Long approverId, String approvedByName, String remark);

    PurchaseExchange reject(Long id, String remark);

    PurchaseExchange cancel(Long id, String reason);

    PurchaseExchange complete(Long id);

    List<PurchaseExchangeItem> getItems(Long exchangeId);

    List<ExchangeApprovalRecord> getApprovalRecords(Long exchangeId);

    Map<String, Object> getTracking(Long exchangeId);
}
