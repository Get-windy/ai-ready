package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote;
import cn.aiedge.erp.purchase.entity.PurchaseQuoteItem;
import cn.aiedge.erp.purchase.dto.QuoteComparisonDTO;
import cn.aiedge.erp.purchase.dto.QuoteComparisonItemDTO;
import cn.aiedge.erp.purchase.mapper.PurchaseSupplierQuoteMapper;
import cn.aiedge.erp.purchase.mapper.PurchaseQuoteItemMapper;
import cn.aiedge.erp.purchase.service.impl.PurchaseQuoteComparisonServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 报价比价分析Service测试
 * 覆盖报价比较、评分计算、推荐生成等核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("报价比价分析测试")
class PurchaseQuoteComparisonTest {

    @Mock
    private PurchaseSupplierQuoteMapper quoteMapper;

    @Mock
    private PurchaseQuoteItemMapper itemMapper;

    @InjectMocks
    private PurchaseQuoteComparisonServiceImpl comparisonService;

    private PurchaseSupplierQuote quote1;
    private PurchaseSupplierQuote quote2;
    private PurchaseSupplierQuote quote3;
    private List<PurchaseQuoteItem> items1;
    private List<PurchaseQuoteItem> items2;
    private List<PurchaseQuoteItem> items3;

    @BeforeEach
    void setUp() {
        // 创建测试报价数据
        quote1 = createQuote(1L, 1L, BigDecimal.valueOf(10000), 90.0, 85.0, 88.0);
        quote2 = createQuote(2L, 2L, BigDecimal.valueOf(9500), 85.0, 90.0, 85.0);
        quote3 = createQuote(3L, 3L, BigDecimal.valueOf(10500), 80.0, 80.0, 90.0);

        // 创建报价明细
        items1 = createQuoteItems(1L, Arrays.asList(
            createItem("物料A", BigDecimal.valueOf(5000), BigDecimal.valueOf(2)),
            createItem("物料B", BigDecimal.valueOf(5000), BigDecimal.valueOf(1))
        ));

        items2 = createQuoteItems(2L, Arrays.asList(
            createItem("物料A", BigDecimal.valueOf(4500), BigDecimal.valueOf(2)),
            createItem("物料B", BigDecimal.valueOf(5000), BigDecimal.valueOf(1))
        ));

        items3 = createQuoteItems(3L, Arrays.asList(
            createItem("物料A", BigDecimal.valueOf(5000), BigDecimal.valueOf(2)),
            createItem("物料B", BigDecimal.valueOf(5500), BigDecimal.valueOf(1))
        ));
    }

    private PurchaseSupplierQuote createQuote(Long id, Long supplierId, BigDecimal totalAmount,
                                              Double priceScore, Double qualityScore, Double serviceScore) {
        PurchaseSupplierQuote quote = new PurchaseSupplierQuote();
        quote.setId(id);
        quote.setInquiryId(100L);
        quote.setSupplierId(supplierId);
        quote.setSupplierName("供应商" + supplierId);
        quote.setTotalAmount(totalAmount);
        quote.setPriceScore(BigDecimal.valueOf(priceScore));
        quote.setQualityScore(BigDecimal.valueOf(qualityScore));
        quote.setServiceScore(BigDecimal.valueOf(serviceScore));
        quote.setTotalScore(BigDecimal.valueOf(
            priceScore * 0.4 + qualityScore * 0.4 + serviceScore * 0.2
        ).setScale(2, RoundingMode.HALF_UP));
        quote.setQuoteStatus(cn.aiedge.erp.purchase.enums.QuoteStatus.SUBMITTED);
        quote.setValidUntil(LocalDateTime.now().plusDays(30));
        return quote;
    }

    private PurchaseQuoteItem createItem(String materialName, BigDecimal unitPrice, BigDecimal quantity) {
        PurchaseQuoteItem item = new PurchaseQuoteItem();
        item.setMaterialName(materialName);
        item.setUnitPrice(unitPrice);
        item.setQuantity(quantity);
        item.setAmount(unitPrice.multiply(quantity));
        return item;
    }

    private List<PurchaseQuoteItem> createQuoteItems(Long quoteId, List<PurchaseQuoteItem> items) {
        items.forEach(item -> item.setQuoteId(quoteId));
        return items;
    }

    @Test
    @DisplayName("多供应商报价比较 - 基本比较功能")
    void testCompareQuotesBasic() {
        // Mock数据
        when(quoteMapper.findByInquiryId(100L)).thenReturn(Arrays.asList(quote1, quote2, quote3));
        when(itemMapper.findByQuoteId(1L)).thenReturn(items1);
        when(itemMapper.findByQuoteId(2L)).thenReturn(items2);
        when(itemMapper.findByQuoteId(3L)).thenReturn(items3);

        // 执行比较
        QuoteComparisonDTO comparison = comparisonService.compareQuotes(100L);

        // 验证结果
        assertNotNull(comparison, "比较结果不应为空");
        assertEquals(100L, comparison.getInquiryId(), "询价单ID应匹配");
        assertEquals(3, comparison.getQuoteList().size(), "应包含3个报价");

        // 验证按总分排序
        List<QuoteComparisonItemDTO> sortedQuotes = comparison.getQuoteList();
        assertTrue(sortedQuotes.stream().allMatch(q -> q.getTotalScore() != null), "所有报价应有总分");

        // 验证推荐供应商（最高分）
        assertNotNull(comparison.getRecommendedSupplierId(), "应有推荐供应商");
        assertEquals(1L, comparison.getRecommendedSupplierId(), "供应商1应被推荐（最高分87.4）");
    }

    @Test
    @DisplayName("报价评分计算 - 权重计算正确性")
    void testCalculateQuoteScore() {
        // Mock数据
        when(quoteMapper.findById(1L)).thenReturn(quote1);

        // 执行评分计算
        PurchaseSupplierQuote scored = comparisonService.calculateQuoteScore(
            1L, 90.0, 85.0, 88.0, "质量优秀，价格合理"
        );

        // 验证评分计算
        assertNotNull(scored.getPriceScore(), "价格评分应不为空");
        assertNotNull(scored.getQualityScore(), "质量评分应不为空");
        assertNotNull(scored.getServiceScore(), "服务评分应不为空");
        assertNotNull(scored.getTotalScore(), "总分应不为空");

        // 验证权重计算：价格40% + 质量40% + 服务20%
        BigDecimal expectedTotal = BigDecimal.valueOf(90.0 * 0.4 + 85.0 * 0.4 + 88.0 * 0.2)
            .setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedTotal, scored.getTotalScore(), "总分权重计算应正确");

        verify(quoteMapper).update(any());
    }

    @Test
    @DisplayName("报价价格对比 - 价格差异分析")
    void testCompareQuotePrices() {
        // Mock数据
        when(quoteMapper.findByInquiryId(100L)).thenReturn(Arrays.asList(quote1, quote2, quote3));

        // 执行价格对比
        String priceAnalysis = comparisonService.compareQuotePrices(100L);

        // 验证分析结果
        assertNotNull(priceAnalysis, "价格分析结果不应为空");
        assertTrue(priceAnalysis.contains("最低报价"), "应包含最低报价信息");
        assertTrue(priceAnalysis.contains("最高报价"), "应包含最高报价信息");
        assertTrue(priceAnalysis.contains("平均报价"), "应包含平均报价信息");

        // 验证价格范围计算
        // 最低：9500 (quote2), 最高：10500 (quote3), 平均：10000
        assertTrue(priceAnalysis.contains("9500"), "最低报价应为9500");
        assertTrue(priceAnalysis.contains("10500"), "最高报价应为10500");
    }

    @Test
    @DisplayName("报价明细对比 - 物料价格明细比较")
    void testCompareQuoteItems() {
        // Mock数据
        when(quoteMapper.findByInquiryId(100L)).thenReturn(Arrays.asList(quote1, quote2, quote3));
        when(itemMapper.findByQuoteId(1L)).thenReturn(items1);
        when(itemMapper.findByQuoteId(2L)).thenReturn(items2);
        when(itemMapper.findByQuoteId(3L)).thenReturn(items3);

        // 执行明细对比
        List<QuoteComparisonItemDTO> itemComparison = comparisonService.compareQuoteItems(100L);

        // 验证结果
        assertNotNull(itemComparison, "明细对比结果不应为空");
        assertFalse(itemComparison.isEmpty(), "明细对比应有数据");

        // 验证包含所有物料
        assertTrue(itemComparison.stream().anyMatch(i -> "物料A".equals(i.getMaterialName())),
            "应包含物料A对比");
        assertTrue(itemComparison.stream().anyMatch(i -> "物料B".equals(i.getMaterialName())),
            "应包含物料B对比");
    }

    @Test
    @DisplayName("报价推荐算法 - 综合评分推荐")
    void testRecommendSupplier() {
        // Mock数据 - 创建不同评分组合的报价
        PurchaseSupplierQuote lowPrice = createQuote(1L, 1L, BigDecimal.valueOf(8000), 95.0, 70.0, 70.0);
        PurchaseSupplierQuote highQuality = createQuote(2L, 2L, BigDecimal.valueOf(12000), 70.0, 95.0, 85.0);
        PurchaseSupplierQuote balanced = createQuote(3L, 3L, BigDecimal.valueOf(10000), 85.0, 85.0, 85.0);

        when(quoteMapper.findByInquiryId(100L)).thenReturn(Arrays.asList(lowPrice, highQuality, balanced));

        // 执行推荐
        Long recommendedId = comparisonService.recommendSupplier(100L);

        // 验证推荐结果
        assertNotNull(recommendedId, "应有推荐供应商");

        // balanced报价总分应为85，最高
        // lowPrice: 95*0.4 + 70*0.4 + 70*0.2 = 38 + 28 + 14 = 80
        // highQuality: 70*0.4 + 95*0.4 + 85*0.2 = 28 + 38 + 17 = 83
        // balanced: 85*0.4 + 85*0.4 + 85*0.2 = 34 + 34 + 17 = 85
        assertEquals(3L, recommendedId, "均衡报价供应商应被推荐");
    }

    @Test
    @DisplayName("报价有效期检查 - 过期报价过滤")
    void testFilterValidQuotes() {
        // Mock数据 - 包含一个过期报价
        PurchaseSupplierQuote expiredQuote = createQuote(4L, 4L, BigDecimal.valueOf(9000), 95.0, 95.0, 95.0);
        expiredQuote.setValidUntil(LocalDateTime.now().minusDays(1)); // 已过期

        when(quoteMapper.findByInquiryId(100L)).thenReturn(Arrays.asList(quote1, quote2, expiredQuote));

        // 执行有效报价过滤
        List<PurchaseSupplierQuote> validQuotes = comparisonService.filterValidQuotes(100L);

        // 验证过滤结果
        assertNotNull(validQuotes, "过滤结果不应为空");
        assertEquals(2, validQuotes.size(), "应过滤掉过期报价");
        assertTrue(validQuotes.stream().noneMatch(q -> q.getId().equals(4L)), "过期报价不应出现在结果中");
    }

    @Test
    @DisplayName("报价历史对比 - 历史价格趋势分析")
    void testCompareWithHistory() {
        // Mock历史报价数据
        PurchaseSupplierQuote historyQuote1 = createQuote(101L, 1L, BigDecimal.valueOf(9000), null, null, null);
        historyQuote1.setCreatedAt(LocalDateTime.now().minusMonths(1));

        PurchaseSupplierQuote historyQuote2 = createQuote(102L, 1L, BigDecimal.valueOf(8500), null, null, null);
        historyQuote2.setCreatedAt(LocalDateTime.now().minusMonths(2));

        when(quoteMapper.findHistoryBySupplierId(1L, 3)).thenReturn(Arrays.asList(historyQuote1, historyQuote2));
        when(quoteMapper.findById(1L)).thenReturn(quote1);

        // 执行历史对比
        String historyAnalysis = comparisonService.compareWithHistory(1L, 3);

        // 验证分析结果
        assertNotNull(historyAnalysis, "历史分析结果不应为空");
        assertTrue(historyAnalysis.contains("历史报价"), "应包含历史报价信息");
        assertTrue(historyAnalysis.contains("价格趋势"), "应包含价格趋势分析");
    }

    @Test
    @DisplayName("报价统计汇总 - 统计数据生成")
    void testGenerateQuoteStatistics() {
        // Mock数据
        when(quoteMapper.findByInquiryId(100L)).thenReturn(Arrays.asList(quote1, quote2, quote3));

        // 执行统计汇总
        var statistics = comparisonService.generateQuoteStatistics(100L);

        // 验证统计结果
        assertNotNull(statistics, "统计数据不应为空");
        assertEquals(3, statistics.getTotalQuotes(), "报价总数应为3");
        assertEquals(BigDecimal.valueOf(9500), statistics.getMinAmount(), "最低报价金额应为9500");
        assertEquals(BigDecimal.valueOf(10500), statistics.getMaxAmount(), "最高报价金额应为10500");

        // 平均报价：10000
        BigDecimal avgAmount = statistics.getAvgAmount();
        assertNotNull(avgAmount, "平均报价应不为空");
        assertEquals(BigDecimal.valueOf(10000).setScale(2, RoundingMode.HALF_UP), avgAmount,
            "平均报价应为10000");
    }

    @Test
    @DisplayName("报价排名生成 - 排名列表生成")
    void testGenerateQuoteRanking() {
        // Mock数据
        when(quoteMapper.findByInquiryId(100L)).thenReturn(Arrays.asList(quote1, quote2, quote3));

        // 执行排名生成
        List<QuoteComparisonItemDTO> ranking = comparisonService.generateQuoteRanking(100L);

        // 验证排名结果
        assertNotNull(ranking, "排名列表不应为空");
        assertEquals(3, ranking.size(), "应有3个排名");

        // 验证按总分降序排列
        for (int i = 0; i < ranking.size() - 1; i++) {
            assertTrue(
                ranking.get(i).getTotalScore().compareTo(ranking.get(i + 1).getTotalScore()) >= 0,
                "排名应按总分降序排列"
            );
        }

        // 验证第一名
        assertEquals(1, ranking.get(0).getRank(), "第一名排名应为1");
        assertEquals(1L, ranking.get(0).getSupplierId(), "第一名应为供应商1");
    }

    @Test
    @DisplayName("报价明细汇总 - 所有报价明细汇总")
    void testSummarizeAllQuoteItems() {
        // Mock数据
        when(quoteMapper.findByInquiryId(100L)).thenReturn(Arrays.asList(quote1, quote2, quote3));
        when(itemMapper.findByQuoteId(1L)).thenReturn(items1);
        when(itemMapper.findByQuoteId(2L)).thenReturn(items2);
        when(itemMapper.findByQuoteId(3L)).thenReturn(items3);

        // 执行明细汇总
        var summary = comparisonService.summarizeAllQuoteItems(100L);

        // 验证汇总结果
        assertNotNull(summary, "汇总结果不应为空");
        assertEquals(2, summary.size(), "应有2种物料（物料A和物料B）");

        // 验证物料汇总包含所有报价的价格信息
        assertTrue(summary.containsKey("物料A"), "应包含物料A汇总");
        assertTrue(summary.containsKey("物料B"), "应包含物料B汇总");
    }

    @Test
    @DisplayName("空报价列表处理 - 边界条件测试")
    void testCompareEmptyQuotes() {
        // Mock空数据
        when(quoteMapper.findByInquiryId(200L)).thenReturn(new ArrayList<>());

        // 执行比较
        QuoteComparisonDTO comparison = comparisonService.compareQuotes(200L);

        // 验证空结果处理
        assertNotNull(comparison, "空结果也应返回对象");
        assertEquals(0, comparison.getQuoteList().size(), "报价列表应为空");
        assertNull(comparison.getRecommendedSupplierId(), "无报价时不应有推荐供应商");
    }

    @Test
    @DisplayName("单报价处理 - 单报价场景测试")
    void testCompareSingleQuote() {
        // Mock单个报价
        when(quoteMapper.findByInquiryId(300L)).thenReturn(Arrays.asList(quote1));
        when(itemMapper.findByQuoteId(1L)).thenReturn(items1);

        // 执行比较
        QuoteComparisonDTO comparison = comparisonService.compareQuotes(300L);

        // 验证单报价处理
        assertNotNull(comparison, "比较结果不应为空");
        assertEquals(1, comparison.getQuoteList().size(), "应有1个报价");
        assertEquals(1L, comparison.getRecommendedSupplierId(), "唯一报价供应商应被推荐");
    }
}