package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote;
import cn.aiedge.erp.purchase.entity.PurchaseQuoteItem;
import java.util.List;

/**
 * 报价Service接口
 */
public interface PurchaseQuoteService {

    /**
     * 提交报价
     */
    PurchaseSupplierQuote submitQuote(PurchaseSupplierQuote quote, List<PurchaseQuoteItem> items);

    /**
     * 更新报价
     */
    PurchaseSupplierQuote updateQuote(Long id, PurchaseSupplierQuote quote);

    /**
     * 审查报价（评分）
     */
    PurchaseSupplierQuote reviewQuote(Long id, Double priceScore, Double qualityScore, 
                                       Double serviceScore, String reviewComment);

    /**
     * 接受报价
     */
    PurchaseSupplierQuote acceptQuote(Long id);

    /**
     * 拒绝报价
     */
    PurchaseSupplierQuote rejectQuote(Long id, String reason);

    /**
     * 撤回报价
     */
    PurchaseSupplierQuote withdrawQuote(Long id);

    /**
     * 根据ID查询报价
     */
    PurchaseSupplierQuote getQuoteById(Long id);

    /**
     * 根据编号查询报价
     */
    PurchaseSupplierQuote getQuoteByNo(String quoteNo);

    /**
     * 根据询价单查询报价列表
     */
    List<PurchaseSupplierQuote> getQuotesByInquiry(Long inquiryId);

    /**
     * 根据供应商查询报价列表
     */
    List<PurchaseSupplierQuote> getQuotesBySupplier(Long supplierId);

    /**
     * 查询询价单的中标报价
     */
    PurchaseSupplierQuote getWinningQuote(Long inquiryId);

    /**
     * 比价分析
     */
    String compareQuotes(Long inquiryId);

    /**
     * 生成报价单编号
     */
    String generateQuoteNo();
}