package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 促销/忠诚程序（对标 Odoo loyalty.program）
 * 统一模型覆盖：满减、优惠券、积分、礼品卡、电子钱包、返券
 */
@Data
@Accessors(chain = true)
@TableName("erp_loyalty_program")
public class LoyaltyProgram {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    /**
     * 程序类型:
     * PROMOTION-满减/满赠（自动触发）
     * COUPON-一次性优惠券
     * DISCOUNT_CODE-优惠码
     * LOYALTY-会员积分卡
     * GIFT_CARD-礼品卡储值
     * EWALLET-电子钱包
     * NEXT_ORDER-下单后返券
     */
    private String programType;
    private String name;
    private String description;

    /** 触发方式: AUTO=自动触发, CODE=需输入码 */
    private String triggerType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer isActive;

    /** 最大使用次数（NULL=不限） */
    private Integer maxUsage;
    /** 已使用次数 */
    private Integer usageCount;

    /**
     * 适用范围:
     * ON_ORDER-整单
     * ON_PRODUCT-指定产品
     * ON_CATEGORY-指定分类
     */
    private String applyScope;

    /** 关联价格表ID（可选） */
    private Long pricelistId;
    private Integer sortOrder;
    private String remark;

    @TableLogic
    private Integer deleted;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
