package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 促销活动实体
 * 支持满减、折扣、赠品、组合等多种促销类型
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_promotion_activity")
public class PromotionActivity {

    /**
     * 促销ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 促销名称
     */
    private String name;

    /**
     * 促销描述
     */
    private String description;

    /**
     * 促销类型：discount-折扣/full_reduction-满减/gift-赠品/combo-组合
     */
    private String type;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 折扣率（0-1）
     */
    private BigDecimal discountRate;

    /**
     * 满减门槛金额
     */
    private BigDecimal minAmount;

    /**
     * 满减金额
     */
    private BigDecimal reductionAmount;

    /**
     * 赠品配置（JSON格式：赠品ID、数量等）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String giftConfig;

    /**
     * 组合配置（JSON格式：组合商品、价格等）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String comboConfig;

    /**
     * 适用客户等级（逗号分隔）
     */
    private String customerLevels;

    /**
     * 适用产品ID（逗号分隔，为空表示全部）
     */
    private String productIds;

    /**
     * 适用区域（逗号分隔）
     */
    private String regions;

    /**
     * 最大使用次数（全局）
     */
    private Integer maxUsageCount;

    /**
     * 每客户限制次数
     */
    private Integer usageLimitPerCustomer;

    /**
     * 是否可与其他促销叠加
     */
    private Boolean stackable;

    /**
     * 状态：draft-草稿/published-已发布/expired-已过期/cancelled-已取消
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
