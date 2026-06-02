package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预算执行日志DTO
 */
@Data
public class BudgetExecutionLogDTO {
    private Long id;
    private Long budgetId;
    private Long budgetItemId;
    private String sourceType;
    private String sourceNo;
    private Long sourceId;
    private BigDecimal amount;
    private String executionType;
    private LocalDate executionDate;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
}
