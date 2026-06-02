package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预算调整DTO
 */
@Data
public class BudgetAdjustmentDTO {
    private Long id;
    private String adjustmentNo;
    private Long budgetId;
    private String adjustmentType;
    private BigDecimal amount;
    private Long sourceSubjectId;
    private String sourceSubjectName;
    private Long targetSubjectId;
    private String targetSubjectName;
    private String reason;
    private String status;
    private String applicantId;
    private String applicantName;
    private String approverId;
    private String approverName;
    private String approvalComment;
    private LocalDate applyDate;
    private LocalDate approvalDate;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
