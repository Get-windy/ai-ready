package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.dto.QuoteComparisonDTO;
import cn.aiedge.erp.purchase.dto.QuoteComparisonItemDTO;
import cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote;

import java.util.List;
import java.util.Map;

/**
 * 报价比价Service接口
 */
public interface PurchaseQuoteComparisonService {

    QuoteComparisonDTO compareQuotes(Long inquiryId);

    Long recommendSupplier(Long inquiryId);

    String compareQuotePrices(Long inquiryId);

    boolean isQuoteValid(Long quoteId);

    PurchaseSupplierQuote calculateQuoteScore(Long quoteId, Double priceScore, Double qualityScore, 
                                              Double serviceScore, String comment);

    List<QuoteComparisonItemDTO> compareQuoteItems(Long inquiryId);

    List<PurchaseSupplierQuote> filterValidQuotes(Long inquiryId);

    String compareWithHistory(Long quoteId, int historyCount);

    QuoteStatisticsDTO generateQuoteStatistics(Long inquiryId);

    List<QuoteComparisonItemDTO> generateQuoteRanking(Long inquiryId);

    Map<String, QuoteComparisonItemDTO> summarizeAllQuoteItems(Long inquiryId);

    /**
     * 报价统计DTO
     */
    interface QuoteStatisticsDTO {
        int getTotalQuotes();
        java.math.BigDecimal getMinAmount();
        java.math.BigDecimal getMaxAmount();
        java.math.BigDecimal getAvgAmount();
    }
}