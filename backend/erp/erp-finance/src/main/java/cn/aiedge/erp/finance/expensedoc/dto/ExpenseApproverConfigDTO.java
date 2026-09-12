package cn.aiedge.erp.finance.expensedoc.dto;

import lombok.Data;

/**
 * 费用审批人配置（按级别默认审批人）
 *
 * 存储于系统参数（sys_config，group=expense_approval）：
 *   expense.approval.level1.approverId / level2 / level3
 * 第一级优先取「费用单所属部门的负责人」，未设置部门负责人时回退到本配置。
 */
@Data
public class ExpenseApproverConfigDTO {

    /** 第一级（部门审批）默认审批人 */
    private Long level1ApproverId;

    /** 第二级（财务审批）默认审批人 */
    private Long level2ApproverId;

    /** 第三级（总经理审批）默认审批人 */
    private Long level3ApproverId;
}
