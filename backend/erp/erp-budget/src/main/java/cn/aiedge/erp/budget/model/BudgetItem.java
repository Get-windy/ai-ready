package cn.aiedge.erp.budget.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 预算科目明细
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "budget_item")
public class BudgetItem extends BaseEntity {

    @Column(name = "budget_id", nullable = false)
    private Long budgetId;

    @Column(name = "subject_code", length = 50)
    private String subjectCode;

    @Column(name = "subject_name", length = 200)
    private String subjectName;

    @Column(name = "budget_amount", precision = 15, scale = 2)
    private BigDecimal budgetAmount;

    @Column(name = "used_amount", precision = 15, scale = 2)
    private BigDecimal usedAmount;

    @Column(name = "remaining_amount", precision = 15, scale = 2)
    private BigDecimal remainingAmount;

    @Column(name = "frozen_amount", precision = 15, scale = 2)
    private BigDecimal frozenAmount;

    @Column(name = "execution_rate", precision = 5, scale = 2)
    private BigDecimal executionRate;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
