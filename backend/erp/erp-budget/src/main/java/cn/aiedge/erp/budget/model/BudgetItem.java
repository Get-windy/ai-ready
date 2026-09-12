package cn.aiedge.erp.budget.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    /** 最近执行日期（冻结/释放/消耗取最近一次） */
    @Column(name = "last_exec_date")
    private LocalDate lastExecDate;

    // ═══ 金标准明细字段 ═══

    /** 行号 */
    @Column(name = "line_no")
    private Integer lineNo;

    /** 预算科目ID（会计科目） */
    @Column(name = "subject_id")
    private Long subjectId;

    /** 科目类别 */
    @Column(name = "subject_type", length = 50)
    private String subjectType;

    /** 备注 */
    @Column(name = "remark", length = 500)
    private String remark;
}
