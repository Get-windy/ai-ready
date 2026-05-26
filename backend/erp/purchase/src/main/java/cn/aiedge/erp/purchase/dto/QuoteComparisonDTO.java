package cn.aiedge.erp.purchase.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 报价比价DTO
 */
@Data
@Accessors(chain = true)
public class QuoteComparisonDTO {

    private Long inquiryId;

    private List<QuoteComparisonItemDTO> quoteList;

    private Long recommendedSupplierId;

    private String comparisonAnalysis;

    private Double priceVariance;

    private Double qualityVariance;

    private Double serviceVariance;

    // 以下为测试代码中使用的字段
    private List<QuoteComparisonItemDTO> quotes;
    
    private String recommendedSupplier;
    
    private String recommendedSupplierName;
    
    private BigDecimal totalSavings;
}