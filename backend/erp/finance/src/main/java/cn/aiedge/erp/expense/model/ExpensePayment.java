package cn.aiedge.erp.expense.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 费用支付实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "expense_payment",
       indexes = {
           @Index(name = "idx_expense_payment_application_id", columnList = "application_id"),
           @Index(name = "idx_expense_payment_status", columnList = "payment_status"),
           @Index(name = "idx_expense_payment_payer_id", columnList = "payer_id")
       })
public class ExpensePayment extends BaseEntity {

    /**
     * 关联的费用申请ID
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /**
     * 支付金额
     */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    /**
     * 支付方式
     */
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    /**
     * 支付状态：PENDING/CONFIRMED/CANCELLED/FAILED
     */
    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus = "PENDING";

    /**
     * 支付日期
     */
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    /**
     * 付款人ID
     */
    @Column(name = "payer_id", length = 50)
    private String payerId;

    /**
     * 付款人姓名
     */
    @Column(name = "payer_name", length = 100)
    private String payerName;

    /**
     * 支付凭证号
     */
    @Column(name = "voucher_no", length = 100)
    private String voucherNo;

    /**
     * 银行流水号
     */
    @Column(name = "bank_transaction_no", length = 100)
    private String bankTransactionNo;
}
