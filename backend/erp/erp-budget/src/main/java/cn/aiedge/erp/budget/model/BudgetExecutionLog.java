package cn.aiedge.erp.budget.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 预算执行日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "budget_execution_log")
public class BudgetExecutionLog extends BaseEntity {

    @Column(name = "budget_id", nullable = false)
    private Long budgetId;

    @Column(name = "budget_item_id")
    private Long budgetItemId;

    @Column(name = "source_type", length = 20)
    private String sourceType;

    @Column(name = "source_no", length = 50)
    private String sourceNo;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "execution_type", length = 20)
    private String executionType;

    @Column(name = "execution_date")
    private LocalDate executionDate;

    @Column(name = "description", length = 500)
    private String description;
}
