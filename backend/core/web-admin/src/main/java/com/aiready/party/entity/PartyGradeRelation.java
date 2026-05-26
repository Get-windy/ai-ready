package com.aiready.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 往来单位等级关系实体类
 * 记录客户与等级的关联关系及升级信息
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_party_grade_relation")
public class PartyGradeRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 往来单位ID
     */
    private Long partyId;

    /**
     * 客户等级ID
     */
    private Long gradeId;

    /**
     * 等级编码（冗余）
     */
    private String gradeCode;

    /**
     * 等级名称（冗余）
     */
    private String gradeName;

    /**
     * 当前累计消费金额
     */
    private BigDecimal totalAmount;

    /**
     * 当前累计订单数
     */
    private Integer totalOrders;

    /**
     * 等级获得时间
     */
    private LocalDateTime obtainTime;

    /**
     * 等级到期时间（如有有效期限制）
     */
    private LocalDateTime expireTime;

    /**
     * 是否永久有效
     */
    private Boolean permanent;

    /**
     * 升级前等级ID
     */
    private Long previousGradeId;

    /**
     * 升级时间
     */
    private LocalDateTime upgradeTime;

    /**
     * 降级时间
     */
    private LocalDateTime downgradeTime;

    /**
     * 降级原因
     */
    private String downgradeReason;

    /**
     * 手动调整标记
     */
    private Boolean manualAdjust;

    /**
     * 调整人ID
     */
    private Long adjustBy;

    /**
     * 调整原因
     */
    private String adjustReason;

    /**
     * 状态（0：失效 1：有效）
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
