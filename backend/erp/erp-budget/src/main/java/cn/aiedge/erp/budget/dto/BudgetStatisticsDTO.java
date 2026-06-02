package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算统计DTO
 */
@Data
public class BudgetStatisticsDTO {
    private BigDecimal totalBudgetAmount;
    private BigDecimal totalUsedAmount;
    private BigDecimal totalRemainingAmount;
    private BigDecimal totalFrozenAmount;
    private BigDecimal executionRate;
    private Integer totalBudgetCount;
    private Integer executingCount;
    private Integer closedCount;
    private Integer draftCount;
    private BigDecimal increaseAmount;
    private BigDecimal decreaseAmount;
}
