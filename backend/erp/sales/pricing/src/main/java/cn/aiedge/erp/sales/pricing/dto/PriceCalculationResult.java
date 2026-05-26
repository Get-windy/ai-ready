package cn.aiedge.erp.sales.pricing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 价格计算结果DTO
 */
@Data
@Schema(description = "价格计算结果DTO")
public class PriceCalculationResult {
    
    @Schema(description = "计算是否成功")
    private boolean success;
    
    @Schema(description = "错误信息")
    private String errorMessage;
    
    @Schema(description = "原始基础价格")
    private BigDecimal originalPrice;
    
    @Schema(description = "最终价格")
    private BigDecimal finalPrice;
    
    @Schema(description = "总折扣金额")
    private BigDecimal totalDiscount;
    
    @Schema(description = "总折扣率")
    private BigDecimal totalDiscountRate;
    
    @Schema(description = "应用的价格策略列表")
    private List<AppliedStrategyInfo> appliedStrategies;
    
    @Schema(description = "计算时间")
    private LocalDateTime calculationTime;
    
    @Schema(description = "计算ID")
    private String calculationId;
    
    @Data
    @Schema(description = "应用的策略信息")
    public static class AppliedStrategyInfo {
        
        @Schema(description = "策略ID")
        private Long strategyId;
        
        @Schema(description = "策略名称")
        private String strategyName;
        
        @Schema(description = "策略类型")
        private String strategyType;
        
        @Schema(description = "应用前的价格")
        private BigDecimal priceBefore;
        
        @Schema(description = "应用后的价格")
        private BigDecimal priceAfter;
        
        @Schema(description = "折扣金额")
        private BigDecimal discountAmount;
        
        @Schema(description = "折扣率")
        private BigDecimal discountRate;
        
        @Schema(description = "应用的规则列表")
        private List<AppliedRuleInfo> appliedRules;
    }
    
    @Data
    @Schema(description = "应用的规则信息")
    public static class AppliedRuleInfo {
        
        @Schema(description = "规则ID")
        private Long ruleId;
        
        @Schema(description = "规则名称")
        private String ruleName;
        
        @Schema(description = "规则类型")
        private String ruleType;
        
        @Schema(description = "规则说明")
        private String ruleDescription;
        
        @Schema(description = "应用的效果")
        private String effect;
    }
}