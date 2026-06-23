package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 促销活动DTO
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "促销活动")
public class PromotionActivityDTO {

    @Schema(description = "促销ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "促销名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "促销描述")
    private String description;

    @Schema(description = "促销类型：discount/full_reduction/gift/combo")
    private String type;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "折扣率")
    private BigDecimal discountRate;

    @Schema(description = "满减门槛金额")
    private BigDecimal minAmount;

    @Schema(description = "满减金额")
    private BigDecimal reductionAmount;

    @Schema(description = "赠品配置（JSON）")
    private String giftConfig;

    @Schema(description = "组合配置（JSON）")
    private String comboConfig;

    @Schema(description = "适用客户等级列表")
    private List<String> customerLevels;

    @Schema(description = "适用产品ID列表")
    private List<Long> productIds;

    @Schema(description = "适用区域列表")
    private List<String> regions;

    @Schema(description = "最大使用次数")
    private Integer maxUsageCount;

    @Schema(description = "每客户限制次数")
    private Integer usageLimitPerCustomer;

    @Schema(description = "是否可叠加")
    private Boolean stackable;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
