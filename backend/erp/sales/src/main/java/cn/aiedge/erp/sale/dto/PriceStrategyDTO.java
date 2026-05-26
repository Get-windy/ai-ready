package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 价格策略DTO
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "价格策略")
public class PriceStrategyDTO {

    @Schema(description = "策略ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "策略名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "策略描述")
    private String description;

    @Schema(description = "策略类型：customer_level/region/product_category/time/composite")
    private String strategyType;

    @Schema(description = "适用客户等级")
    private String customerLevel;

    @Schema(description = "适用区域编码")
    private String regionCode;

    @Schema(description = "适用产品类别ID")
    private Long productCategoryId;

    @Schema(description = "基础价格")
    private BigDecimal basePrice;

    @Schema(description = "价格系数")
    private BigDecimal priceFactor;

    @Schema(description = "折扣率")
    private BigDecimal discountRate;

    @Schema(description = "折扣金额")
    private BigDecimal discountAmount;

    @Schema(description = "最小数量门槛")
    private Integer minQuantity;

    @Schema(description = "公式配置（JSON）")
    private String formulaConfig;

    @Schema(description = "生效开始时间")
    private LocalDateTime effectiveStartTime;

    @Schema(description = "生效结束时间")
    private LocalDateTime effectiveEndTime;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "关联规则列表")
    private List<PriceRuleDTO> rules;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
