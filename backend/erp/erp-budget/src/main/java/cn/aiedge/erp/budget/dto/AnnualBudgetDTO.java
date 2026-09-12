package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private BigDecimal totalFrozenAmount;
    private BigDecimal executionRate;
    private String description;
    private String remark;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private List<BudgetItemDTO> items;

    // ═══ 金标准编制/审批字段 ═══
    private LocalDate budgetDate;
    private Long handlerId;
    private String handlerName;
    private String creatorName;
    private Long auditorId;
    private String auditorName;
    private LocalDateTime auditTime;
    private String auditRemark;
    private Integer printCount;
}
