package cn.aiedge.erp.budget.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 预算调整
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "budget_adjustment")
public class BudgetAdjustment extends BaseEntity {

    @Column(name = "adjustment_no", unique = true, length = 50)
    private String adjustmentNo;

    @Column(name = "budget_id", nullable = false)
    private Long budgetId;

    @Column(name = "adjustment_type", length = 20)
    private String adjustmentType;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "source_subject_id")
    private Long sourceSubjectId;

    @Column(name = "target_subject_id")
    private Long targetSubjectId;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "applicant_id", length = 50)
    private String applicantId;

    @Column(name = "applicant_name", length = 100)
    private String applicantName;

    @Column(name = "approver_id", length = 50)
    private String approverId;

    @Column(name = "approver_name", length = 100)
    private String approverName;

    @Column(name = "approval_comment", length = 500)
    private String approvalComment;

    @Column(name = "apply_date")
    private LocalDate applyDate;

    @Column(name = "approval_date")
    private LocalDate approvalDate;
}
