package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 价格计算结果DTO
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "价格计算结果")
public class PriceCalculationResult {

    @Schema(description = "原始总价（未应用任何策略）")
    private BigDecimal originalTotalAmount;

    @Schema(description = "折扣后总价")
    private BigDecimal discountedTotalAmount;

    @Schema(description = "附加费用总额")
    private BigDecimal additionalCharges;

    @Schema(description = "税费")
    private BigDecimal taxAmount;

    @Schema(description = "最终总价")
    private BigDecimal finalTotalAmount;

    @Schema(description = "总折扣金额")
    private BigDecimal totalDiscountAmount;

    @Schema(description = "明细列表")
    private List<PriceCalculationItemResult> itemResults;

    @Schema(description = "应用的策略列表")
    private List<AppliedStrategyInfo> appliedStrategies;

    @Schema(description = "应用的促销活动列表")
    private List<AppliedPromotionInfo> appliedPromotions;

    /**
     * 明细项计算结果
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "明细项计算结果")
    public static class PriceCalculationItemResult {

        @Schema(description = "产品ID")
        private Long productId;

        @Schema(description = "产品名称")
        private String productName;

        @Schema(description = "基础单价")
        private BigDecimal basePrice;

        @Schema(description = "数量")
        private Integer quantity;

        @Schema(description = "小计（基础）")
        private BigDecimal subtotal;

        @Schema(description = "折扣后单价")
        private BigDecimal discountedUnitPrice;

        @Schema(description = "折扣后小计")
        private BigDecimal discountedSubtotal;

        @Schema(description = "折扣金额")
        private BigDecimal discountAmount;

        @Schema(description = "应用的规则列表")
        private List<String> appliedRules;
    }

    /**
     * 应用的策略信息
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "应用的策略信息")
    public static class AppliedStrategyInfo {

        @Schema(description = "策略ID")
        private Long strategyId;

        @Schema(description = "策略名称")
        private String strategyName;

        @Schema(description = "策略类型")
        private String strategyType;

        @Schema(description = "折扣金额")
        private BigDecimal discountAmount;
    }

    /**
     * 应用的促销信息
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "应用的促销信息")
    public static class AppliedPromotionInfo {

        @Schema(description = "促销ID")
        private Long promotionId;

        @Schema(description = "促销名称")
        private String promotionName;

        @Schema(description = "促销类型")
        private String promotionType;

        @Schema(description = "优惠金额")
        private BigDecimal discountAmount;
    }
}
