package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote;
import cn.aiedge.erp.purchase.entity.PurchaseQuoteItem;
import cn.aiedge.erp.purchase.enums.QuoteStatus;
import cn.aiedge.erp.purchase.mapper.PurchaseSupplierQuoteMapper;
import cn.aiedge.erp.purchase.mapper.PurchaseInquiryMapper;
import cn.aiedge.erp.purchase.service.PurchaseQuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Comparator;

/**
 * 报价Service实现
 */
@Service
@RequiredArgsConstructor
public class PurchaseQuoteServiceImpl implements PurchaseQuoteService {

    private final PurchaseSupplierQuoteMapper quoteMapper;
    private final PurchaseInquiryMapper inquiryMapper;

    @Override
    @Transactional
    public PurchaseSupplierQuote submitQuote(PurchaseSupplierQuote quote, List<PurchaseQuoteItem> items) {
        quote.setQuoteNo(generateQuoteNo());
        quote.setQuoteStatus(QuoteStatus.SUBMITTED);
        quote.setQuoteDate(LocalDateTime.now());
        
        // 计算总金额
        BigDecimal totalAmount = items.stream()
            .map(PurchaseQuoteItem::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        quote.setTotalAmount(totalAmount);
        
        quoteMapper.insert(quote);
        
        // 更新询价单的报价数量
        int count = quoteMapper.countByInquiryId(quote.getInquiryId());
        inquiryMapper.updateQuoteCount(quote.getInquiryId(), count);
        
        return quote;
    }

    @Override
    @Transactional
    public PurchaseSupplierQuote updateQuote(Long id, PurchaseSupplierQuote quote) {
        PurchaseSupplierQuote existing = quoteMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("报价单不存在");
        }
        quote.setId(id);
        quoteMapper.update(quote);
        return quoteMapper.findById(id);
    }

    @Override
    @Transactional
    public PurchaseSupplierQuote reviewQuote(Long id, Double priceScore, Double qualityScore, 
                                             Double serviceScore, String reviewComment) {
        PurchaseSupplierQuote quote = quoteMapper.findById(id);
        if (quote == null) {
            throw new RuntimeException("报价单不存在");
        }
        
        // 设置评分
        quote.setPriceScore(BigDecimal.valueOf(priceScore));
        quote.setQualityScore(BigDecimal.valueOf(qualityScore));
        quote.setServiceScore(BigDecimal.valueOf(serviceScore));
        
        // 计算综合评分（价格40% + 质量40% + 服务20%）
        BigDecimal totalScore = quote.getPriceScore().multiply(BigDecimal.valueOf(0.4))
            .add(quote.getQualityScore().multiply(BigDecimal.valueOf(0.4)))
            .add(quote.getServiceScore().multiply(BigDecimal.valueOf(0.2)));
        quote.setTotalScore(totalScore);
        
        quote.setReviewStatus("REVIEWED");
        quote.setReviewDate(LocalDateTime.now());
        quote.setReviewComment(reviewComment);
        
        quoteMapper.update(quote);
        return quote;
    }

    @Override
    @Transactional
    public PurchaseSupplierQuote acceptQuote(Long id) {
        PurchaseSupplierQuote quote = quoteMapper.findById(id);
        if (quote == null) {
            throw new RuntimeException("报价单不存在");
        }
        quote.setQuoteStatus(QuoteStatus.ACCEPTED);
        quote.setIsRecommended(true);
        quoteMapper.update(quote);
        return quote;
    }

    @Override
    @Transactional
    public PurchaseSupplierQuote rejectQuote(Long id, String reason) {
        PurchaseSupplierQuote quote = quoteMapper.findById(id);
        if (quote == null) {
            throw new RuntimeException("报价单不存在");
        }
        quote.setQuoteStatus(QuoteStatus.REJECTED);
        quote.setRecommendReason(reason);
        quoteMapper.update(quote);
        return quote;
    }

    @Override
    @Transactional
    public PurchaseSupplierQuote withdrawQuote(Long id) {
        PurchaseSupplierQuote quote = quoteMapper.findById(id);
        if (quote == null) {
            throw new RuntimeException("报价单不存在");
        }
        quote.setQuoteStatus(QuoteStatus.WITHDRAWN);
        quoteMapper.update(quote);
        return quote;
    }

    @Override
    public PurchaseSupplierQuote getQuoteById(Long id) {
        return quoteMapper.findById(id);
    }

    @Override
    public PurchaseSupplierQuote getQuoteByNo(String quoteNo) {
        return quoteMapper.findByQuoteNo(quoteNo);
    }

    @Override
    public List<PurchaseSupplierQuote> getQuotesByInquiry(Long inquiryId) {
        return quoteMapper.findByInquiryId(inquiryId);
    }

    @Override
    public List<PurchaseSupplierQuote> getQuotesBySupplier(Long supplierId) {
        return quoteMapper.findBySupplierId(supplierId);
    }

    @Override
    public PurchaseSupplierQuote getWinningQuote(Long inquiryId) {
        return quoteMapper.findTopByInquiryId(inquiryId);
    }

    @Override
    public String compareQuotes(Long inquiryId) {
        List<PurchaseSupplierQuote> quotes = quoteMapper.findByInquiryId(inquiryId);
        if (quotes.isEmpty()) {
            return "暂无报价";
        }
        
        // 比价分析：按综合评分排序
        quotes.sort(Comparator.comparing(PurchaseSupplierQuote::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder())));
        
        StringBuilder analysis = new StringBuilder();
        analysis.append("询价单" + inquiryId + "比价分析结果：\n");
        for (int i = 0; i < quotes.size(); i++) {
            PurchaseSupplierQuote q = quotes.get(i);
            analysis.append(String.format("%d. 供应商%d: 总额%.2f, 综合评分%.2f\n", 
                i+1, q.getSupplierId(), q.getTotalAmount(), q.getTotalScore()));
        }
        
        if (!quotes.isEmpty()) {
            PurchaseSupplierQuote best = quotes.get(0);
            analysis.append(String.format("推荐供应商%d，报价%.2f，评分%.2f\n", 
                best.getSupplierId(), best.getTotalAmount(), best.getTotalScore()));
        }
        
        return analysis.toString();
    }

    @Override
    public String generateQuoteNo() {
        String prefix = "QT";
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", (int)(Math.random() * 10000));
        return prefix + datePart + randomPart;
    }

    public boolean isQuoteValid(Long quoteId) {
        PurchaseSupplierQuote quote = quoteMapper.findById(quoteId);
        if (quote == null) {
            return false;
        }
        return quote.getValidUntil() != null && quote.getValidUntil().isAfter(LocalDateTime.now());
    }
}