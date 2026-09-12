package cn.aiedge.erp.budget.dto;

import lombok.Data;

/**
 * 预算执行查询条件
 */
@Data
public class BudgetExecutionQuery {

    /** 财政年度 */
    private Integer fiscalYear;

    /** 部门ID */
    private String departmentId;

    /** 预算科目编码 */
    private String subjectCode;

    /** 关键字：预算编号 / 部门名称 / 预算科目名称 */
    private String keyword;

    /** 预算状态：draft/submitted/approved/executing/rejected/closed，空=已转入执行(approved,executing,closed) */
    private String status;

    /** 是否仅看超支（执行进度 > 100%） */
    private Boolean overBudgetOnly;

    /** 是否仅看预警（执行进度 >= 90%） */
    private Boolean warningOnly;

    /** 页码，从 0 开始 */
    private int page = 0;

    /** 每页大小 */
    private int size = 20;
}
