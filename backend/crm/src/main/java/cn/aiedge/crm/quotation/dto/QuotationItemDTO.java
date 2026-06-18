package cn.aiedge.crm.quotation.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuotationItemDTO {

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private Long productCategoryId;

    private String categoryName;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal costPrice;

    private BigDecimal discountRate;

    private BigDecimal taxRate;

    private String description;

    private String remark;

    private Integer deliveryDays;

    private String warrantyTerms;

    private String serviceTerms;
}