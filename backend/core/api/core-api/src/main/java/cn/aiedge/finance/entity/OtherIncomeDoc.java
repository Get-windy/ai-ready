package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 其他收入单实体
 * Ref: Odoo 18.0 account.move - misc income entries
 */
@Data
@Accessors(chain = true)
@TableName("fin_other_income_doc")
public class OtherIncomeDoc {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String docNo;
    /** 收入类型: INTEREST/RENT/PENALTY/INSURANCE/OTHER */
    private String incomeType;
    private BigDecimal amount;
    private String currency;
    private Long partnerId;
    private String partnerName;
    private LocalDate incomeDate;
    /** 收入来源说明 */
    private String source;
    /** 状态: 0-草稿 1-待审核 2-已审核 3-已入账 4-已作废 */
    private Integer status;
    private String settlementMethod;
    private String bankAccount;
    private String bankName;
    private String transactionNo;
    private Long departmentId;
    private String departmentName;
    private Long creatorId;
    private String creatorName;
    private String remark;
    private Long approvedBy;
    private LocalDateTime approvedTime;
    private String approvedNote;

    @TableLogic
    private Integer deleted;
    @Version
    private Integer versionNo;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
