package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.erp.purchase.dto.QuoteComparisonDTO;
import cn.aiedge.erp.purchase.dto.QuoteComparisonItemDTO;
import cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote;
import cn.aiedge.erp.purchase.entity.PurchaseQuoteItem;
import cn.aiedge.erp.purchase.mapper.PurchaseSupplierQuoteMapper;
import cn.aiedge.erp.purchase.mapper.PurchaseQuoteItemMapper;
import cn.aiedge.erp.purchase.service.PurchaseQuoteComparisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报价比价Service实现
 */
@Service
@RequiredArgsConstructor
public class PurchaseQuoteComparisonServiceImpl implements PurchaseQuoteComparisonService {

    private final PurchaseSupplierQuoteMapper quoteMapper;
    private final PurchaseQuoteItemMapper quoteItemMapper;

    @Override
    public QuoteComparisonDTO compareQuotes(Long inquiryId) {
        List<PurchaseSupplierQuote> quotes = quoteMapper.findByInquiryId(inquiryId);
        
        QuoteComparisonDTO comparison = new QuoteComparisonDTO();
        comparison.setInquiryId(inquiryId);
        
        List<QuoteComparisonItemDTO> itemDTOList = quotes.stream()
            .map(this::convertToItemDTO)
            .sorted(Comparator.comparing(QuoteComparisonItemDTO::getTotalScore, Comparator.reverseOrder()))
            .collect(Collectors.toList());
        comparison.setQuoteList(itemDTOList);
        
        if (!quotes.isEmpty()) {
            PurchaseSupplierQuote best = quotes.stream()
                .max(Comparator.comparing(PurchaseSupplierQuote::getTotalScore))
                .orElse(null);
            comparison.setRecommendedSupplierId(best != null ? best.getSupplierId() : null);
        }
        
        return comparison;
    }

    private QuoteComparisonItemDTO convertToItemDTO(PurchaseSupplierQuote quote) {
        QuoteComparisonItemDTO dto = new QuoteComparisonItemDTO();
        dto.setQuoteId(quote.getId());
        dto.setSupplierId(quote.getSupplierId());
        dto.setSupplierName(quote.getSupplierName());
        dto.setTotalAmount(quote.getTotalAmount());
        dto.setPriceScore(quote.getPriceScore());
        dto.setQualityScore(quote.getQualityScore());
        dto.setServiceScore(quote.getServiceScore());
        dto.setTotalScore(quote.getTotalScore());
        return dto;
    }

    @Override
    public Long recommendSupplier(Long inquiryId) {
        PurchaseSupplierQuote best = quoteMapper.findTopByInquiryId(inquiryId);
        return best != null ? best.getSupplierId() : null;
    }

    @Override
    public String compareQuotePrices(Long inquiryId) {
        List<PurchaseSupplierQuote> quotes = quoteMapper.findByInquiryId(inquiryId);
        
        StringBuilder analysis = new StringBuilder();
        analysis.append("价格对比分析：\n");
        
        BigDecimal minAmount = quotes.stream()
            .map(PurchaseSupplierQuote::getTotalAmount)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        
        BigDecimal maxAmount = quotes.stream()
            .map(PurchaseSupplierQuote::getTotalAmount)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        
        BigDecimal avgAmount = quotes.stream()
            .map(PurchaseSupplierQuote::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(quotes.size()), 2, RoundingMode.HALF_UP);
        
        analysis.append("最低报价: ").append(minAmount).append("\n");
        analysis.append("最高报价: ").append(maxAmount).append("\n");
        analysis.append("平均报价: ").append(avgAmount).append("\n");
        
        quotes.stream()
            .sorted(Comparator.comparing(PurchaseSupplierQuote::getTotalAmount))
            .forEach(q -> analysis.append(String.format("供应商%d: %.2f\n", q.getSupplierId(), q.getTotalAmount())));
        
        return analysis.toString();
    }

    @Override
    public boolean isQuoteValid(Long quoteId) {
        PurchaseSupplierQuote quote = quoteMapper.findById(quoteId);
        if (quote == null) {
            return false;
        }
        return quote.getValidUntil() != null && quote.getValidUntil().isAfter(LocalDateTime.now());
    }

    @Override
    public PurchaseSupplierQuote calculateQuoteScore(Long quoteId, Double priceScore, Double qualityScore,
                                                     Double serviceScore, String comment) {
        PurchaseSupplierQuote quote = quoteMapper.findById(quoteId);
        if (quote == null) {
            throw new IllegalArgumentException("报价不存在");
        }
        
        quote.setPriceScore(BigDecimal.valueOf(priceScore));
        quote.setQualityScore(BigDecimal.valueOf(qualityScore));
        quote.setServiceScore(BigDecimal.valueOf(serviceScore));
        
        BigDecimal totalScore = BigDecimal.valueOf(priceScore * 0.4 + qualityScore * 0.4 + serviceScore * 0.2)
            .setScale(2, RoundingMode.HALF_UP);
        quote.setTotalScore(totalScore);
        
        quoteMapper.update(quote);
        return quote;
    }

    @Override
    public List<QuoteComparisonItemDTO> compareQuoteItems(Long inquiryId) {
        List<PurchaseSupplierQuote> quotes = quoteMapper.findByInquiryId(inquiryId);
        
        List<QuoteComparisonItemDTO> result = new ArrayList<>();
        for (PurchaseSupplierQuote quote : quotes) {
            List<PurchaseQuoteItem> items = quoteItemMapper.findByQuoteId(quote.getId());
            for (PurchaseQuoteItem item : items) {
                QuoteComparisonItemDTO dto = new QuoteComparisonItemDTO();
                dto.setSupplierId(quote.getSupplierId());
                dto.setSupplierName(quote.getSupplierName());
                dto.setMaterialName(item.getMaterialName());
                dto.setUnitPrice(item.getUnitPrice());
                dto.setQuantity(item.getQuantity());
                dto.setAmount(item.getAmount());
                result.add(dto);
            }
        }
        
        return result;
    }

    @Override
    public List<PurchaseSupplierQuote> filterValidQuotes(Long inquiryId) {
        return quoteMapper.findByInquiryId(inquiryId).stream()
            .filter(q -> q.getValidUntil() != null && q.getValidUntil().isAfter(LocalDateTime.now()))
            .collect(Collectors.toList());
    }

    @Override
    public String compareWithHistory(Long quoteId, int historyCount) {
        PurchaseSupplierQuote current = quoteMapper.findById(quoteId);
        if (current == null) {
            return "报价不存在";
        }
        
        List<PurchaseSupplierQuote> history = quoteMapper.findHistoryBySupplierId(current.getSupplierId(), historyCount);
        
        StringBuilder analysis = new StringBuilder();
        analysis.append("历史报价对比分析：\n");
        analysis.append("当前报价: ").append(current.getTotalAmount()).append("\n");
        
        if (!history.isEmpty()) {
            analysis.append("历史报价趋势:\n");
            for (PurchaseSupplierQuote h : history) {
                analysis.append(String.format("  %s: %.2f\n", 
                    h.getCreatedAt() != null ? h.getCreatedAt().toLocalDate() : "未知日期", 
                    h.getTotalAmount()));
            }
        }
        
        return analysis.toString();
    }

    @Override
    public QuoteStatisticsDTO generateQuoteStatistics(Long inquiryId) {
        List<PurchaseSupplierQuote> quotes = quoteMapper.findByInquiryId(inquiryId);
        
        return new QuoteStatisticsDTO() {
            @Override
            public int getTotalQuotes() {
                return quotes.size();
            }
            
            @Override
            public BigDecimal getMinAmount() {
                return quotes.stream()
                    .map(PurchaseSupplierQuote::getTotalAmount)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            }
            
            @Override
            public BigDecimal getMaxAmount() {
                return quotes.stream()
                    .map(PurchaseSupplierQuote::getTotalAmount)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            }
            
            @Override
            public BigDecimal getAvgAmount() {
                if (quotes.isEmpty()) return BigDecimal.ZERO;
                return quotes.stream()
                    .map(PurchaseSupplierQuote::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(quotes.size()), 2, RoundingMode.HALF_UP);
            }
        };
    }

    @Override
    public List<QuoteComparisonItemDTO> generateQuoteRanking(Long inquiryId) {
        List<PurchaseSupplierQuote> quotes = quoteMapper.findByInquiryId(inquiryId);
        
        List<QuoteComparisonItemDTO> ranking = quotes.stream()
            .map(this::convertToItemDTO)
            .sorted(Comparator.comparing(QuoteComparisonItemDTO::getTotalScore, Comparator.reverseOrder()))
            .collect(Collectors.toList());
        
        int rank = 1;
        for (QuoteComparisonItemDTO item : ranking) {
            item.setRank(rank++);
        }
        
        return ranking;
    }

    @Override
    public Map<String, QuoteComparisonItemDTO> summarizeAllQuoteItems(Long inquiryId) {
        List<PurchaseSupplierQuote> quotes = quoteMapper.findByInquiryId(inquiryId);
        
        Map<String, QuoteComparisonItemDTO> summary = new HashMap<>();
        
        for (PurchaseSupplierQuote quote : quotes) {
            List<PurchaseQuoteItem> items = quoteItemMapper.findByQuoteId(quote.getId());
            for (PurchaseQuoteItem item : items) {
                String materialName = item.getMaterialName();
                summary.computeIfAbsent(materialName, k -> {
                    QuoteComparisonItemDTO dto = new QuoteComparisonItemDTO();
                    dto.setMaterialName(k);
                    return dto;
                });
            }
        }
        
        return summary;
    }
}