package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务交易记录实体
 * 记录企业所有财务交易
 */
@Data
@TableName("finance_transaction")
@EqualsAndHashCode(callSuper = true)
public class FinanceTransaction extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 交易编号
     */
    @TableField("transaction_no")
    private String transactionNo;

    /**
     * 交易类型
     * 1-收入 2-支出 3-转账 4-退款
     */
    @TableField("transaction_type")
    private Integer transactionType;

    /**
     * 交易金额
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 交易时间
     */
    @TableField("transaction_time")
    private LocalDateTime transactionTime;

    /**
     * 收入账户ID
     */
    @TableField("credit_account_id")
    private Long creditAccountId;

    /**
     * 支出账户ID
     */
    @TableField("debit_account_id")
    private Long debitAccountId;

    /**
     * 关联业务类型
     * 1-采购 2-销售 3-费用报销 4-工资发放 5-税务缴纳 6-其他
     */
    @TableField("biz_type")
    private Integer bizType;

    /**
     * 关联业务ID
     */
    @TableField("biz_id")
    private Long bizId;

    /**
     * 交易描述
     */
    @TableField("description")
    private String description;

    /**
     * 交易状态
     * 0-待处理 1-已处理 2-已撤销
     */
    @TableField("status")
    private Integer status = 1;

    /**
     * 凭证编号
     */
    @TableField("voucher_no")
    private String voucherNo;

    /**
     * 附件URL
     */
    @TableField("attachment_url")
    private String attachmentUrl;

    /**
     * 审核人ID
     */
    @TableField("approved_by")
    private String approvedBy;

    /**
     * 审核时间
     */
    @TableField("approved_at")
    private LocalDateTime approvedAt;
}
