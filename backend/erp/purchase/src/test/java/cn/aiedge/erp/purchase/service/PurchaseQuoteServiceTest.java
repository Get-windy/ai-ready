package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote;
import cn.aiedge.erp.purchase.entity.PurchaseQuoteItem;
import cn.aiedge.erp.purchase.enums.QuoteStatus;
import cn.aiedge.erp.purchase.mapper.PurchaseSupplierQuoteMapper;
import cn.aiedge.erp.purchase.mapper.PurchaseInquiryMapper;
import cn.aiedge.erp.purchase.service.impl.PurchaseQuoteServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 报价Service测试
 */
@ExtendWith(MockitoExtension.class)
class PurchaseQuoteServiceTest {

    @Mock
    private PurchaseSupplierQuoteMapper quoteMapper;

    @Mock
    private PurchaseInquiryMapper inquiryMapper;

    @InjectMocks
    private PurchaseQuoteServiceImpl quoteService;

    @Test
    void testSubmitQuote() {
        PurchaseSupplierQuote quote = new PurchaseSupplierQuote();
        quote.setInquiryId(1L);
        quote.setSupplierId(1L);
        quote.setValidUntil(LocalDateTime.now().plusDays(30));
        quote.setCreatedBy(1L);

        PurchaseQuoteItem item1 = new PurchaseQuoteItem();
        item1.setUnitPrice(BigDecimal.valueOf(100));
        item1.setQuantity(BigDecimal.valueOf(10));

        PurchaseQuoteItem item2 = new PurchaseQuoteItem();
        item2.setUnitPrice(BigDecimal.valueOf(200));
        item2.setQuantity(BigDecimal.valueOf(5));

        List<PurchaseQuoteItem> items = Arrays.asList(item1, item2);

        when(quoteMapper.insert(any())).thenReturn(1);
        when(quoteMapper.countByInquiryId(1L)).thenReturn(1);
        when(inquiryMapper.updateQuoteCount(1L, 1)).thenReturn(1);

        PurchaseSupplierQuote submitted = quoteService.submitQuote(quote, items);
        
        assertNotNull(submitted);
        assertEquals(QuoteStatus.SUBMITTED, submitted.getQuoteStatus());
        assertNotNull(submitted.getQuoteNo());
        assertEquals(BigDecimal.valueOf(2000), submitted.getTotalAmount()); // 100*10 + 200*5
        verify(quoteMapper).insert(any());
    }

    @Test
    void testReviewQuote() {
        PurchaseSupplierQuote quote = new PurchaseSupplierQuote();
        quote.setId(1L);

        when(quoteMapper.findById(1L)).thenReturn(quote);
        when(quoteMapper.update(any())).thenReturn(1);

        PurchaseSupplierQuote reviewed = quoteService.reviewQuote(1L, 90.0, 85.0, 80.0, "测试评分");
        
        assertNotNull(reviewed.getPriceScore());
        assertNotNull(reviewed.getQualityScore());
        assertNotNull(reviewed.getServiceScore());
        assertNotNull(reviewed.getTotalScore());
        assertEquals(BigDecimal.valueOf(87.0), reviewed.getTotalScore()); // 90*0.4 + 85*0.4 + 80*0.2
        verify(quoteMapper).update(any());
    }

    @Test
    void testCompareQuotes() {
        PurchaseSupplierQuote quote1 = new PurchaseSupplierQuote();
        quote1.setSupplierId(1L);
        quote1.setTotalAmount(BigDecimal.valueOf(1000));
        quote1.setTotalScore(BigDecimal.valueOf(90));

        PurchaseSupplierQuote quote2 = new PurchaseSupplierQuote();
        quote2.setSupplierId(2L);
        quote2.setTotalAmount(BigDecimal.valueOf(1200));
        quote2.setTotalScore(BigDecimal.valueOf(85));

        when(quoteMapper.findByInquiryId(1L)).thenReturn(Arrays.asList(quote1, quote2));

        String analysis = quoteService.compareQuotes(1L);
        
        assertNotNull(analysis);
        assertTrue(analysis.contains("推荐供应商1"));
        verify(quoteMapper).findByInquiryId(1L);
    }
}