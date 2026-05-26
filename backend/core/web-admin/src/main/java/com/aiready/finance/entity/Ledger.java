package com.aiready.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账簿记录实体类（明细账/总账）
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("fin_ledger")
public class Ledger {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 账簿类型（1：明细账 2：总账）
     */
    private Integer ledgerType;
    
    /**
     * 科目ID
     */
    private Long subjectId;
    
    /**
     * 科目编码
     */
    private String subjectCode;
    
    /**
     * 科目名称
     */
    private String subjectName;
    
    /**
     * 会计期间
     */
    private String accountingPeriod;
    
    /**
     * 业务日期
     */
    private LocalDate businessDate;
    
    /**
     * 凭证ID
     */
    private Long voucherId;
    
    /**
     * 凭证字号
     */
    private String voucherNo;
    
    /**
     * 分录ID
     */
    private Long voucherItemId;
    
    /**
     * 摘要
     */
    private String summary;
    
    /**
     * 借方金额
     */
    private BigDecimal debitAmount;
    
    /**
     * 贷方金额
     */
    private BigDecimal creditAmount;
    
    /**
     * 余额
     */
    private BigDecimal balance;
    
    /**
     * 借贷方向（1：借 2：贷）
     */
    private Integer balanceDirection;
    
    /**
     * 辅助核算-客户ID
     */
    private Long customerId;
    
    /**
     * 辅助核算-供应商ID
     */
    private Long supplierId;
    
    /**
     * 辅助核算-部门ID
     */
    private Long departmentId;
    
    /**
     * 辅助核算-项目ID
     */
    private Long projectId;
    
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
