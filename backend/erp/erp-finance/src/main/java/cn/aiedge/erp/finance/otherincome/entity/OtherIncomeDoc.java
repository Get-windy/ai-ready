package cn.aiedge.erp.finance.otherincome.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 其他收入单实体（金标准）
 * 非主营收入登记，与费用单对称。
 * 收入类型：1-往来单位收入 2-内部收入。
 */
@Data
@Accessors(chain = true)
@TableName("fin_other_income_doc")
public class OtherIncomeDoc {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 收入单号（QTSRD- 前缀） */
    private String docNo;

    /** 收入类型: 1-往来单位收入 2-内部收入 */
    private Integer incomeType;

    /** 本单金额 */
    private BigDecimal amount;

    private String currency;

    /** 往来单位ID(往来单位收入) */
    private Long partnerId;

    /** 往来单位 */
    private String partnerName;

    /** 往来编号 */
    private String partnerCode;

    private LocalDate incomeDate;

    /** 经手人ID */
    private Long handlerId;

    /** 经手人 */
    private String handlerName;

    /** 部门ID */
    private Long departmentId;

    private String departmentName;

    /** 收款账户1 */
    private String receiptAccount1;

    private BigDecimal receiptAmount1;

    /** 收款账户2 */
    private String receiptAccount2;

    private BigDecimal receiptAmount2;

    /** 收款账户3 */
    private String receiptAccount3;

    private BigDecimal receiptAmount3;

    /** 收款账户4 */
    private String receiptAccount4;

    private BigDecimal receiptAmount4;

    /** 收款账户科目(凭证借方) */
    private String accountSubjectCode;

    /** 摘要 */
    private String summary;

    /** 附件 */
    private String attachment;

    /** 状态: 0-草稿 1-已记账/已入账 */
    private Integer status;

    /** 结算状态: 0-未结算 1-已结算 */
    private Integer settleStatus;

    /** 来源说明 */
    private String source;

    private String settlementMethod;

    private String bankAccount;

    private String bankName;

    private String transactionNo;

    private Long creatorId;

    private String creatorName;

    /** 记账人ID */
    private Long bookkeeperId;

    /** 记账人名称 */
    private String bookkeeperName;

    /** 记账时间 */
    private LocalDateTime bookkeepingTime;

    /** 打印次数 */
    private Integer printCount;

    /** 凭证号（KJPZ-，记账后生成） */
    private String voucherNo;

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
