package cn.aiedge.erp.budget.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 年度预算（预算编制单）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "annual_budget")
public class AnnualBudget extends BaseEntity {

    @Column(name = "budget_no", unique = true, length = 50)
    private String budgetNo;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "template_name", length = 200)
    private String templateName;

    @Column(name = "fiscal_year")
    private Integer fiscalYear;

    @Column(name = "department_id", length = 50)
    private String departmentId;

    @Column(name = "department_name", length = 200)
    private String departmentName;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "total_approved_amount", precision = 15, scale = 2)
    private BigDecimal totalApprovedAmount;

    @Column(name = "total_used_amount", precision = 15, scale = 2)
    private BigDecimal totalUsedAmount;

    @Column(name = "total_remaining_amount", precision = 15, scale = 2)
    private BigDecimal totalRemainingAmount;

    /** 冻结金额汇总（占用待复核）：剩余 = 预算 − 已执行 − 冻结 */
    @Column(name = "total_frozen_amount", precision = 15, scale = 2)
    private BigDecimal totalFrozenAmount;

    @Column(name = "execution_rate", precision = 5, scale = 2)
    private BigDecimal executionRate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "remark", length = 500)
    private String remark;

    // ═══ 金标准编制/审批字段 ═══

    /** 编制日期 */
    @Column(name = "budget_date")
    private LocalDate budgetDate;

    /** 经手人 */
    @Column(name = "handler_id")
    private Long handlerId;

    @Column(name = "handler_name", length = 100)
    private String handlerName;

    /** 制单人姓名（快照） */
    @Column(name = "creator_name", length = 100)
    private String creatorName;

    /** 审核人 */
    @Column(name = "auditor_id")
    private Long auditorId;

    @Column(name = "auditor_name", length = 100)
    private String auditorName;

    @Column(name = "audit_time")
    private LocalDateTime auditTime;

    /** 审批意见 */
    @Column(name = "audit_remark", length = 500)
    private String auditRemark;

    /** 打印次数 */
    @Column(name = "print_count")
    private Integer printCount;
}
