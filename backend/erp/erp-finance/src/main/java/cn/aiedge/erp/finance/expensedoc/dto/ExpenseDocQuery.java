package cn.aiedge.erp.finance.expensedoc.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 费用单多条件分页查询条件
 * 按单据/按明细两套搜索字段。
 */
@Data
public class ExpenseDocQuery {

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 20;

    /** 单据日期范围 */
    private LocalDate dateStart;

    private LocalDate dateEnd;

    /** 单据编号 */
    private String docNo;

    /** 往来单位 */
    private String partnerName;

    /** 经手人 */
    private String handlerName;

    /** 部门 */
    private String deptName;

    /** 制单人 */
    private String creatorName;

    /** 记账人 */
    private String bookkeeperName;

    /** 审核人 */
    private String auditorName;

    /** 单据状态 */
    private Integer status;

    /** 费用类别 0-往来单位费用 1-内部费用 */
    private Integer expenseType;

    /** 审批状态 0-未提交 1-审批中 2-审批通过 3-审批驳回 */
    private Integer approvalStatus;

    /** 付款账户 */
    private String payAccountName;

    /** 摘要 */
    private String summary;

    /** 单据备注 */
    private String remark;

    /** 费用科目（按明细分页专用） */
    private String subjectCode;

    /** 费用名称（按明细分页专用） */
    private String expenseName;

    /** 明细备注（按明细分页专用） */
    private String itemRemark;

    /** 显示红冲 */
    private Boolean showRed;
}
