package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 价格规则DTO
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "价格规则")
public class PriceRuleDTO {

    @Schema(description = "规则ID")
    private Long id;

    @Schema(description = "规则名称")
    private String name;

    @Schema(description = "规则描述")
    private String description;

    @Schema(description = "所属策略ID")
    private Long strategyId;

    @Schema(description = "规则类型")
    private String ruleType;

    @Schema(description = "条件表达式")
    private String conditionExpression;

    @Schema(description = "计算表达式")
    private String calculationExpression;

    @Schema(description = "折扣率")
    private BigDecimal discountRate;

    @Schema(description = "折扣金额")
    private BigDecimal discountAmount;

    @Schema(description = "价格系数")
    private BigDecimal priceFactor;

    @Schema(description = "固定金额")
    private BigDecimal fixedAmount;

    @Schema(description = "最小数量")
    private Integer minQuantity;

    @Schema(description = "最大数量")
    private Integer maxQuantity;

    @Schema(description = "最小金额")
    private BigDecimal minAmount;

    @Schema(description = "最大金额")
    private BigDecimal maxAmount;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态")
    private String status;
}
