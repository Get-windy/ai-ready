package com.aiready.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会计凭证实体类
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("fin_voucher")
public class Voucher {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 凭证字号（如：记-001）
     */
    private String voucherNo;
    
    /**
     * 凭证日期
     */
    private LocalDate voucherDate;
    
    /**
     * 会计期间（如：2026-04）
     */
    private String accountingPeriod;
    
    /**
     * 凭证类型（1：收款 2：付款 3：转账）
     */
    private Integer voucherType;
    
    /**
     * 凭证摘要
     */
    private String summary;
    
    /**
     * 借方金额合计
     */
    private BigDecimal totalDebit;
    
    /**
     * 贷方金额合计
     */
    private BigDecimal totalCredit;
    
    /**
     * 附件张数
     */
    private Integer attachmentCount;
    
    /**
     * 制单人ID
     */
    private Long creatorId;
    
    /**
     * 审核人ID
     */
    private Long reviewerId;
    
    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;
    
    /**
     * 记账人ID
     */
    private Long bookkeeperId;
    
    /**
     * 记账时间
     */
    private LocalDateTime bookkeepingTime;
    
    /**
     * 状态（0：草稿 1：待审核 2：已审核 3：已记账 4：已作废）
     */
    private Integer status;
    
    /**
     * 来源单据类型
     */
    private String sourceType;
    
    /**
     * 来源单据ID
     */
    private Long sourceId;
    
    /**
     * 备注
     */
    private String remark;
    
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
