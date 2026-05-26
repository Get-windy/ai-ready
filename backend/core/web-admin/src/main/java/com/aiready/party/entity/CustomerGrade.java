package com.aiready.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户等级实体类
 * 用于管理不同客户等级及其权益
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_customer_grade")
public class CustomerGrade {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 等级编码
     */
    private String gradeCode;

    /**
     * 等级名称
     */
    private String gradeName;

    /**
     * 等级级别（1-5，数字越大等级越高）
     */
    private Integer gradeLevel;

    /**
     * 等级图标
     */
    private String gradeIcon;

    /**
     * 等级颜色
     */
    private String gradeColor;

    /**
     * 最低消费金额要求
     */
    private BigDecimal minAmount;

    /**
     * 最高消费金额限制
     */
    private BigDecimal maxAmount;

    /**
     * 最低价格限制
     */
    private BigDecimal minPrice;

    /**
     * 最高价格限制
     */
    private BigDecimal maxPrice;

    /**
     * 折扣率（0-100，表示百分比）
     */
    private BigDecimal discountRate;

    /**
     * 积分倍率
     */
    private BigDecimal pointRate;

    /**
     * 信用额度
     */
    private BigDecimal creditLimit;

    /**
     * 账期天数
     */
    private Integer creditDays;

    /**
     * 是否享受包邮
     */
    private Boolean freeShipping;

    /**
     * 包邮最低金额
     */
    private BigDecimal freeShippingMinAmount;

    /**
     * 生日特权
     */
    private String birthdayPrivilege;

    /**
     * 专属客服
     */
    private Boolean exclusiveService;

    /**
     * 优先发货
     */
    private Boolean priorityShipping;

    /**
     * 等级描述
     */
    private String description;

    /**
     * 排序号
     */
    private Integer sortOrder;

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