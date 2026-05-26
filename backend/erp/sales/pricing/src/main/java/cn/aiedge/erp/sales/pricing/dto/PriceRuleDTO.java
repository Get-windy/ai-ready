package cn.aiedge.erp.sales.pricing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 价格规则DTO
 */
@Data
@Schema(description = "价格规则DTO")
public class PriceRuleDTO {
    
    @Schema(description = "规则ID")
    private Long id;
    
    @Schema(description = "策略ID", required = true)
    private Long strategyId;
    
    @Schema(description = "规则名称")
    private String ruleName;
    
    @Schema(description = "规则类型", required = true)
    private String ruleType;
    
    @Schema(description = "条件类型")
    private String conditionType;
    
    @Schema(description = "条件值")
    private String conditionValue;
    
    @Schema(description = "计算类型")
    private String calculationType;
    
    @Schema(description = "价格系数")
    private BigDecimal priceFactor;
    
    @Schema(description = "折扣率")
    private BigDecimal discountRate;
    
    @Schema(description = "折扣金额")
    private BigDecimal discountAmount;
    
    @Schema(description = "最小数量")
    private Integer minQuantity;
    
    @Schema(description = "最大数量")
    private Integer maxQuantity;
    
    @Schema(description = "最小金额")
    private BigDecimal minAmount;
    
    @Schema(description = "最大金额")
    private BigDecimal maxAmount;
    
    @Schema(description = "规则优先级")
    private Integer priority = 0;
    
    @Schema(description = "状态")
    private String status = "active";
}