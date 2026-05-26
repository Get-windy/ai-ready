package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格策略实体
 * 支持多维度价格策略配置：客户等级、区域、产品类别、时间
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_pricing_strategy")
public class PriceStrategy {

    /**
     * 策略ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 策略名称
     */
    private String name;

    /**
     * 策略描述
     */
    private String description;

    /**
     * 策略类型：customer_level-客户等级/region-区域/product_category-产品类别/time-时间/composite-组合
     */
    private String strategyType;

    /**
     * 适用客户等级：strategic-战略/core-核心/normal-普通
     */
    private String customerLevel;

    /**
     * 适用区域编码
     */
    private String regionCode;

    /**
     * 适用产品类别ID
     */
    private Long productCategoryId;

    /**
     * 基础价格
     */
    private BigDecimal basePrice;

    /**
     * 价格系数（用于区域、时间等系数调整）
     */
    private BigDecimal priceFactor;

    /**
     * 折扣率（0-1，如0.05表示5%折扣）
     */
    private BigDecimal discountRate;

    /**
     * 折扣金额（直接减免金额）
     */
    private BigDecimal discountAmount;

    /**
     * 最小数量门槛（数量折扣用）
     */
    private Integer minQuantity;

    /**
     * 公式配置（JSON格式，存储价格计算公式）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String formulaConfig;

    /**
     * 生效开始时间
     */
    private LocalDateTime effectiveStartTime;

    /**
     * 生效结束时间
     */
    private LocalDateTime effectiveEndTime;

    /**
     * 优先级（数字越小优先级越高，用于多策略冲突时选择）
     */
    private Integer priority;

    /**
     * 状态：active-生效/inactive-失效/draft-草稿
     */
    private String status;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
