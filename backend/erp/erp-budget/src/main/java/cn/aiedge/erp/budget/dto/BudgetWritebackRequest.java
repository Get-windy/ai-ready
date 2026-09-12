package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务单据回写预算执行请求
 *
 * <p>费用、支出类单据记账成功后，按「会计年度 + 部门 + 费用科目编码」匹配预算科目，
 * 将单据金额计入对应预算科目的「已执行」。</p>
 */
@Data
public class BudgetWritebackRequest {

    /** 来源单据类型：expense 费用单 / other 其他支出 */
    private String sourceType;

    /** 来源单号 */
    private String sourceNo;

    /** 来源单据ID */
    private Long sourceId;

    /** 会计年度（取单据日期所属年度） */
    private Integer fiscalYear;

    /** 部门ID（与 annual_budget.department_id 对齐；为空表示不限部门） */
    private String departmentId;

    /** 执行日期 */
    private LocalDate execDate;

    /** 单据金额明细（按费用科目） */
    private List<BudgetWritebackItem> items = new ArrayList<>();
}
