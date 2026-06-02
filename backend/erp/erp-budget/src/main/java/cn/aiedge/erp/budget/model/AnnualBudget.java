package cn.aiedge.erp.budget.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 年度预算
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

    @Column(name = "execution_rate", precision = 5, scale = 2)
    private BigDecimal executionRate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "remark", length = 500)
    private String remark;
}
