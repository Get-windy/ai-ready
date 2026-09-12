package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 预算执行行（按预算科目明细维度）
 *
 * <p>口径：剩余 = 预算 − 已执行 − 冻结；执行进度 = 已执行 / 预算 × 100%。</p>
 */
@Data
public class BudgetExecutionItemDTO {

    /** 预算科目明细ID */
    private Long id;
    private Long budgetId;
    private String budgetNo;
    private Integer fiscalYear;
    private String departmentId;
    private String departmentName;
    private String status;
    private String statusName;

    private Integer lineNo;
    private String subjectCode;
    private String subjectName;

    /** 预算金额 */
    private BigDecimal budgetAmount;
    /** 已执行 */
    private BigDecimal usedAmount;
    /** 冻结（占用待复核） */
    private BigDecimal frozenAmount;
    /** 剩余 */
    private BigDecimal remainingAmount;
    /** 执行进度(%) */
    private BigDecimal executionRate;

    /** 超支金额（执行 − 预算，>0 表示超支） */
    private BigDecimal overAmount;
    /** 最近执行日期 */
    private LocalDate lastExecDate;
    /** 执行笔数 */
    private Integer execCount;
    /** 备注 */
    private String remark;

    /** 是否超支（执行进度 > 100%） */
    private Boolean overBudget;
    /** 预警级别：normal 正常 / warn 预警(≥90%) / over 超支(>100%) */
    private String warnLevel;
    /** 预警文案 */
    private String warnMessage;
}
