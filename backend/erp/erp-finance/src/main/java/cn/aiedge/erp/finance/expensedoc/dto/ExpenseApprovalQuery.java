package cn.aiedge.erp.finance.expensedoc.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 费用审批（待审批工作台）多条件分页查询条件
 */
@Data
public class ExpenseApprovalQuery {

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

    /** 费用类型 0-往来单位费用 1-内部费用 */
    private Integer expenseType;

    /** 制单人 */
    private String creatorName;

    /** 审批状态 0-未提交 1-审批中 2-审批通过 3-审批驳回 */
    private Integer approvalStatus;

    /** 当前审批人（模糊） */
    private String currentApproverName;

    /** 摘要 */
    private String summary;

    /** 仅看我的待办（按当前登录人 current_approver_id 过滤） */
    private Boolean onlyMine;
}
