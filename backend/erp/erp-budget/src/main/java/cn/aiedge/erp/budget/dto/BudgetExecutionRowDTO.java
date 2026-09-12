package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 预算执行行（按预算单维度）
 *
 * <p>口径：剩余 = 预算 − 已执行 − 冻结；执行进度 = 已执行 / 预算 × 100%。</p>
 */
@Data
public class BudgetExecutionRowDTO {

    private Long budgetId;
    private String budgetNo;
    private Integer fiscalYear;
    private String departmentId;
    private String departmentName;

    /** 预算单名称（取描述） */
    private String budgetName;
    private String status;
    private String statusName;

    /** 预算金额 */
    private BigDecimal totalAmount;
    /** 已执行 */
    private BigDecimal usedAmount;
    /** 冻结（占用待复核） */
    private BigDecimal frozenAmount;
    /** 剩余 */
    private BigDecimal remainingAmount;
    /** 执行进度(%) */
    private BigDecimal executionRate;

    /** 预算科目数 */
    private Integer itemCount;
    /** 超支科目数 */
    private Integer overItemCount;
    /** 最近执行日期 */
    private LocalDate lastExecDate;

    /** 是否超支（执行进度 > 100%） */
    private Boolean overBudget;
    /** 预警级别：normal 正常 / warn 预警(≥90%) / over 超支(>100%) */
    private String warnLevel;
    /** 预警文案 */
    private String warnMessage;
}
