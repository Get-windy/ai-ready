package cn.aiedge.erp.expense.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 费用审批记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "expense_approval",
       indexes = {
           @Index(name = "idx_expense_approval_application_id", columnList = "application_id"),
           @Index(name = "idx_expense_approval_approver_id", columnList = "approver_id"),
           @Index(name = "idx_expense_approval_approval_time", columnList = "approval_time")
       })
public class ExpenseApproval extends BaseEntity {

    @Column(name = "application_id", nullable = false, length = 50)
    private String applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", insertable = false, updatable = false)
    private ExpenseApplication expenseApplication;

    @Column(name = "approval_level", nullable = false)
    private Integer approvalLevel;

    @Column(name = "approver_id", nullable = false, length = 50)
    private String approverId;

    @Column(name = "approver_name", nullable = false, length = 100)
    private String approverName;

    @Column(name = "approver_department_id", length = 50)
    private String approverDepartmentId;

    @Column(name = "approver_department_name", length = 100)
    private String approverDepartmentName;

    @Column(name = "approval_action", nullable = false, length = 20)
    private String approvalAction;

    @Column(name = "approval_comment", length = 1000)
    private String approvalComment;

    @Column(name = "approval_time", nullable = false)
    private LocalDateTime approvalTime;

    @Column(name = "previous_status", length = 30)
    private String previousStatus;

    @Column(name = "current_status", length = 30)
    private String currentStatus;

    @Column(name = "remark", length = 500)
    private String remark;
}