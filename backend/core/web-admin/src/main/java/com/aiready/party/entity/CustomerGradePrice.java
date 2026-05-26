package com.aiready.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户等级价格实体类
 * 用于设置不同客户等级对应的产品价格
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_customer_grade_price")
public class CustomerGradePrice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 客户等级ID
     */
    private Long gradeId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 产品SKU ID
     */
    private Long skuId;

    /**
     * 产品名称（冗余存储）
     */
    private String productName;

    /**
     * 产品规格（冗余存储）
     */
    private String productSpec;

    /**
     * 标准售价
     */
    private BigDecimal standardPrice;

    /**
     * 等级价格
     */
    private BigDecimal gradePrice;

    /**
     * 价格类型（1：固定价格 2：折扣率）
     */
    private Integer priceType;

    /**
     * 折扣率（当priceType=2时使用）
     */
    private BigDecimal discountRate;

    /**
     * 最低限价
     */
    private BigDecimal minPrice;

    /**
     * 最高限价
     */
    private BigDecimal maxPrice;

    /**
     * 生效时间
     */
    private LocalDateTime effectiveTime;

    /**
     * 失效时间
     */
    private LocalDateTime expireTime;

    /**
     * 是否默认价格
     */
    private Boolean isDefault;

    /**
     * 优先级（数字越大优先级越高）
     */
    private Integer priority;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态（0：禁用 1：启用）
     */
    private Integer status;

    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 删除标志（0：未删除 1：已删除）
     */
    @TableLogic
    private Integer deleted;
}
