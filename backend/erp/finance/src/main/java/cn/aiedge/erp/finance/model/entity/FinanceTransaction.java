package cn.aiedge.erp.finance.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务交易记录实体
 * 记录企业所有财务交易
 */
@Data
@Entity
@Table(name = "finance_transaction")
@EqualsAndHashCode(callSuper = true)
public class FinanceTransaction extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 交易编号
     */
    @Column(name = "transaction_no", nullable = false, unique = true, length = 32)
    private String transactionNo;
    
    /**
     * 交易类型
     * 1-收入 2-支出 3-转账 4-退款
     */
    @Column(name = "transaction_type", nullable = false)
    private Integer transactionType;
    
    /**
     * 交易金额
     */
    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    
    /**
     * 交易时间
     */
    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;
    
    /**
     * 收入账户ID
     */
    @Column(name = "credit_account_id")
    private Long creditAccountId;
    
    /**
     * 支出账户ID
     */
    @Column(name = "debit_account_id")
    private Long debitAccountId;
    
    /**
     * 关联业务类型
     * 1-采购 2-销售 3-费用报销 4-工资发放 5-税务缴纳 6-其他
     */
    @Column(name = "biz_type", nullable = false)
    private Integer bizType;
    
    /**
     * 关联业务ID
     */
    @Column(name = "biz_id")
    private Long bizId;
    
    /**
     * 交易描述
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * 交易状态
     * 0-待处理 1-已处理 2-已撤销
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;
    
    /**
     * 凭证编号
     */
    @Column(name = "voucher_no", length = 32)
    private String voucherNo;
    
    /**
     * 附件URL
     */
    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;
    
    /**
     * 审核人ID
     */
    @Column(name = "approved_by", length = 64)
    private String approvedBy;
    
    /**
     * 审核时间
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
