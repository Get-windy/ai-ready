package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 业务单据回写预算执行的单行金额（按费用科目）
 */
@Data
public class BudgetWritebackItem {

    /** 费用科目编码（与 budget_item.subject_code 对齐） */
    private String subjectCode;

    /** 费用科目名称 */
    private String subjectName;

    /** 金额 */
    private BigDecimal amount;
}
