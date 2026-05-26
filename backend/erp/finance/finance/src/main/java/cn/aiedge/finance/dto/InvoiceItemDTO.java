package cn.aiedge.finance.dto;

import lombok.Data;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 发票明细DTO
 */
@Data
public class InvoiceItemDTO {
    
    private Long id;
    
    @NotBlank(message = "商品名称不能为空")
    private String goodsName;
    
    private String specification;
    private String unit;
    
    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0", message = "数量不能为负数")
    private BigDecimal quantity;
    
    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0", message = "单价不能为负数")
    private BigDecimal unitPrice;
    
    private BigDecimal amount;
    
    @NotNull(message = "税率不能为空")
    @DecimalMin(value = "0", message = "税率不能为负数")
    @DecimalMax(value = "1", message = "税率不能超过100%")
    private BigDecimal taxRate;
    
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    
    private String taxCategoryCode;
    private Integer preferential;
    private String preferentialContent;
    private Integer zeroTaxFlag;
}
