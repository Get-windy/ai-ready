package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算科目DTO
 */
@Data
public class BudgetItemDTO {
    private Long id;
    private Long budgetId;
    private String subjectCode;
    private String subjectName;
    private BigDecimal budgetAmount;
    private BigDecimal usedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal frozenAmount;
    private BigDecimal executionRate;
    private Integer sortOrder;
}
