package cn.aiedge.erp.purchase.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 报价比价明细DTO
 */
@Data
@Accessors(chain = true)
public class QuoteComparisonItemDTO {

    private Long quoteId;

    private Long supplierId;

    private String supplierName;

    private BigDecimal totalAmount;

    private BigDecimal priceScore;

    private BigDecimal qualityScore;

    private BigDecimal serviceScore;

    private BigDecimal totalScore;

    private Integer rank;

    private String materialName;

    private BigDecimal unitPrice;

    private BigDecimal quantity;

    private BigDecimal amount;
}