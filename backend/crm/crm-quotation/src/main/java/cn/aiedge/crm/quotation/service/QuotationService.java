package cn.aiedge.crm.quotation.service;

import cn.aiedge.crm.quotation.entity.Quotation;
import cn.aiedge.crm.quotation.entity.QuotationItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface QuotationService extends IService<Quotation> {

    Quotation getByQuotationNo(String quotationNo);

    Page<Quotation> pageList(String keyword, Long customerId, Long opportunityId, Integer status, Long salesPersonId, int pageNum, int pageSize);

    List<Quotation> listByCustomerId(Long customerId);

    List<Quotation> listByOpportunityId(Long opportunityId);

    List<Quotation> listVersions(Long parentId);

    String generateQuotationNo();

    Quotation createQuotation(Quotation quotation, List<QuotationItem> items);

    Quotation createFromOpportunity(Long opportunityId);

    Quotation createFromTemplate(Long templateId, Long customerId);

    Quotation copyQuotation(Long quotationId);

    Quotation createNewVersion(Long quotationId);

    Quotation updateQuotation(Long quotationId, Quotation quotation, List<QuotationItem> items);

    Quotation submitForApproval(Long quotationId);

    Quotation approve(Long quotationId, Long approverId, String note);

    Quotation reject(Long quotationId, Long rejecterId, String reason);

    Quotation sendToCustomer(Long quotationId, Long senderId, String method);

    Quotation markAccepted(Long quotationId, Long accepterId, String note);

    Quotation markRejected(Long quotationId, Long rejecterId, String reason);

    Quotation convertToOrder(Long quotationId);

    Quotation cancel(Long quotationId, String reason);

    void calculateAmounts(Long quotationId);

    BigDecimal calculateLineAmount(QuotationItem item);

    List<Quotation> getExpiredQuotations();

    void markExpiredQuotations();

    QuotationItem addItem(Long quotationId, QuotationItem item);

    QuotationItem updateItem(Long itemId, QuotationItem item);

    void removeItem(Long itemId);

    List<QuotationItem> getItems(Long quotationId);

    void reorderItems(Long quotationId, List<Long> itemIds);
}