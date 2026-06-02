package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算模板科目DTO
 */
@Data
public class BudgetTemplateItemDTO {
    private Long id;
    private Long templateId;
    private String subjectCode;
    private String subjectName;
    private BigDecimal budgetAmount;
    private Integer sortOrder;
}
