package cn.aiedge.erp.expense.model;

import cn.aiedge.erp.expense.model.enumeration.ExpenseStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 费用报销实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "expense_reimbursement",
       indexes = {
           @Index(name = "idx_expense_reimbursement_application_id", columnList = "application_id"),
           @Index(name = "idx_expense_reimbursement_applicant_id", columnList = "applicant_id"),
           @Index(name = "idx_expense_reimbursement_status", columnList = "status")
       })
public class ExpenseReimbursement extends BaseEntity {

    /**
     * 关联的费用申请ID
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /**
     * 报销金额
     */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    /**
     * 报销类型
     */
    @Column(name = "reimbursement_type", length = 50)
    private String reimbursementType;

    /**
     * 状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExpenseStatus status = ExpenseStatus.DRAFT;

    /**
     * 银行账户
     */
    @Column(name = "bank_account", length = 100)
    private String bankAccount;

    /**
     * 银行名称
     */
    @Column(name = "bank_name", length = 200)
    private String bankName;

    /**
     * 申请人ID
     */
    @Column(name = "applicant_id", nullable = false, length = 50)
    private String applicantId;

    /**
     * 申请人姓名
     */
    @Column(name = "applicant_name", nullable = false, length = 100)
    private String applicantName;

    /**
     * 部门ID
     */
    @Column(name = "department_id", length = 50)
    private String departmentId;

    /**
     * 部门名称
     */
    @Column(name = "department_name", length = 100)
    private String departmentName;

    /**
     * 申请日期
     */
    @Column(name = "apply_date")
    private LocalDate applyDate;

    /**
     * 报销凭证号
     */
    @Column(name = "voucher_no", length = 100)
    private String voucherNo;

    /**
     * 支付方式
     */
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;
}
