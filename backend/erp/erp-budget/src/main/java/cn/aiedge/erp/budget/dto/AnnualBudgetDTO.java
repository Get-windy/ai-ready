package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 年度预算DTO
 */
@Data
public class AnnualBudgetDTO {
    private Long id;
    private String budgetNo;
    private Long templateId;
    private String templateName;
    private Integer fiscalYear;
    private String departmentId;
    private String departmentName;
    private BigDecimal totalAmount;
    private String status;
    private BigDecimal totalApprovedAmount;
    private BigDecimal totalUsedAmount;
    private BigDecimal totalRemainingAmount;
    private BigDecimal executionRate;
    private String description;
    private String remark;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private List<BudgetItemDTO> items;
}
