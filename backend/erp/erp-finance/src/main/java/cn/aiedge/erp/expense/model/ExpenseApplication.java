package cn.aiedge.erp.expense.model;

import cn.aiedge.erp.expense.model.enumeration.ExpenseStatus;
import cn.aiedge.erp.expense.model.enumeration.ExpenseType;
import cn.aiedge.erp.expense.model.enumeration.PaymentMethod;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 费用申请主表实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "expense_application", 
       indexes = {
           @Index(name = "idx_expense_application_code", columnList = "application_code"),
           @Index(name = "idx_expense_application_status", columnList = "status"),
           @Index(name = "idx_expense_application_apply_date", columnList = "apply_date"),
           @Index(name = "idx_expense_application_applicant_id", columnList = "applicant_id"),
           @Index(name = "idx_expense_application_department_id", columnList = "department_id")
       })
public class ExpenseApplication extends BaseEntity {
    
    /**
     * 费用申请编号
     */
    @Column(name = "application_code", nullable = false, unique = true, length = 50)
    private String applicationCode;
    
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
    @Column(name = "apply_date", nullable = false)
    private LocalDate applyDate;
    
    /**
     * 费用类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "expense_type", nullable = false, length = 20)
    private ExpenseType expenseType;
    
    /**
     * 费用类型描述（冗余字段，便于查询）
     */
    @Column(name = "expense_type_desc", length = 100)
    private String expenseTypeDesc;
    
    /**
     * 费用总金额
     */
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    /**
     * 币种
     */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "CNY";
    
    /**
     * 预算科目ID
     */
    @Column(name = "budget_subject_id", length = 50)
    private String budgetSubjectId;
    
    /**
     * 预算科目名称
     */
    @Column(name = "budget_subject_name", length = 200)
    private String budgetSubjectName;
    
    /**
     * 预算金额
     */
    @Column(name = "budget_amount", precision = 15, scale = 2)
    private BigDecimal budgetAmount;
    
    /**
     * 已使用预算金额
     */
    @Column(name = "used_budget_amount", precision = 15, scale = 2)
    private BigDecimal usedBudgetAmount = BigDecimal.ZERO;
    
    /**
     * 预算使用率
     */
    @Column(name = "budget_usage_rate", precision = 5, scale = 2)
    private BigDecimal budgetUsageRate = BigDecimal.ZERO;
    
    /**
     * 费用事由
     */
    @Column(name = "purpose", nullable = false, length = 500)
    private String purpose;
    
    /**
     * 详细说明
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * 状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExpenseStatus status = ExpenseStatus.DRAFT;
    
    /**
     * 状态描述
     */
    @Column(name = "status_desc", length = 100)
    private String statusDesc;
    
    /**
     * 当前审批人ID
     */
    @Column(name = "current_approver_id", length = 50)
    private String currentApproverId;
    
    /**
     * 当前审批人姓名
     */
    @Column(name = "current_approver_name", length = 100)
    private String currentApproverName;
    
    /**
     * 当前审批级别
     */
    @Column(name = "current_approval_level")
    private Integer currentApprovalLevel = 0;
    
    /**
     * 总审批级别
     */
    @Column(name = "total_approval_level")
    private Integer totalApprovalLevel = 1;
    
    /**
     * 工作流实例ID
     */
    @Column(name = "process_instance_id", length = 100)
    private String processInstanceId;
    
    /**
     * 工作流定义ID
     */
    @Column(name = "process_definition_id", length = 100)
    private String processDefinitionId;
    
    /**
     * 工作流任务ID
     */
    @Column(name = "task_id", length = 100)
    private String taskId;
    
    /**
     * 支付方式
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;
    
    /**
     * 支付账户
     */
    @Column(name = "payment_account", length = 100)
    private String paymentAccount;
    
    /**
     * 支付日期
     */
    @Column(name = "payment_date")
    private LocalDate paymentDate;
    
    /**
     * 支付凭证号
     */
    @Column(name = "payment_voucher_no", length = 100)
    private String paymentVoucherNo;
    
    /**
     * 报销日期
     */
    @Column(name = "reimbursement_date")
    private LocalDate reimbursementDate;
    
    /**
     * 报销凭证号
     */
    @Column(name = "reimbursement_voucher_no", length = 100)
    private String reimbursementVoucherNo;
    
    /**
     * 附件数量
     */
    @Column(name = "attachment_count")
    private Integer attachmentCount = 0;
    
    /**
     * 是否紧急
     */
    @Column(name = "is_urgent")
    private Boolean isUrgent = false;
    
    /**
     * 紧急原因
     */
    @Column(name = "urgent_reason", length = 500)
    private String urgentReason;
    
    /**
     * 预计完成日期
     */
    @Column(name = "expected_completion_date")
    private LocalDate expectedCompletionDate;
    
    /**
     * 实际完成日期
     */
    @Column(name = "actual_completion_date")
    private LocalDate actualCompletionDate;
    
    /**
     * 是否超出预算
     */
    @Column(name = "exceed_budget")
    private Boolean exceedBudget = false;
    
    /**
     * 超出预算金额
     */
    @Column(name = "exceed_amount", precision = 15, scale = 2)
    private BigDecimal exceedAmount = BigDecimal.ZERO;
    
    /**
     * 超出预算原因
     */
    @Column(name = "exceed_reason", length = 500)
    private String exceedReason;
    
    /**
     * 审批意见
     */
    @Column(name = "approval_comment", length = 1000)
    private String approvalComment;
    
    /**
     * 拒绝原因
     */
    @Column(name = "reject_reason", length = 1000)
    private String rejectReason;
    
    /**
     * 取消原因
     */
    @Column(name = "cancel_reason", length = 1000)
    private String cancelReason;
    
    /**
     * 费用明细项列表
     */
    @OneToMany(mappedBy = "expenseApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExpenseItem> expenseItems = new ArrayList<>();
    
    /**
     * 审批记录列表
     */
    @OneToMany(mappedBy = "expenseApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExpenseApproval> approvalRecords = new ArrayList<>();
    
    /**
     * 附件列表
     */
    @OneToMany(mappedBy = "expenseApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExpenseAttachment> attachments = new ArrayList<>();
    
    /**
     * 计算总金额
     */
    public void calculateTotalAmount() {
        if (expenseItems != null && !expenseItems.isEmpty()) {
            this.totalAmount = expenseItems.stream()
                .map(ExpenseItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            this.totalAmount = BigDecimal.ZERO;
        }
    }
    
    /**
     * 检查是否超出预算
     */
    public void checkBudgetExceed() {
        if (budgetAmount != null && totalAmount != null) {
            this.exceedBudget = totalAmount.compareTo(budgetAmount) > 0;
            if (this.exceedBudget) {
                this.exceedAmount = totalAmount.subtract(budgetAmount);
            } else {
                this.exceedAmount = BigDecimal.ZERO;
            }
        }
    }
    
    /**
     * 计算预算使用率
     */
    public void calculateBudgetUsageRate() {
        if (budgetAmount != null && budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.budgetUsageRate = usedBudgetAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(budgetAmount, 2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.budgetUsageRate = BigDecimal.ZERO;
        }
    }
    
    /**
     * 更新费用类型描述
     */
    @PrePersist
    @PreUpdate
    public void updateExpenseTypeDesc() {
        if (expenseType != null) {
            this.expenseTypeDesc = expenseType.getDescription();
        }
    }
    
    /**
     * 更新状态描述
     */
    public void updateStatusDesc() {
        if (status != null) {
            this.statusDesc = status.getDescription();
        }
    }
    
    /**
     * 是否可以提交审批
     */
    public boolean canSubmit() {
        return status.canSubmit() && totalAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * 是否可以审批
     */
    public boolean canApprove() {
        return status.canApprove();
    }
    
    /**
     * 是否可以支付
     */
    public boolean canPay() {
        return status.canPay();
    }
    
    /**
     * 是否可以取消
     */
    public boolean canCancel() {
        return status.canCancel();
    }
    
    /**
     * 是否是终态
     */
    public boolean isTerminal() {
        return status.isTerminal();
    }
}