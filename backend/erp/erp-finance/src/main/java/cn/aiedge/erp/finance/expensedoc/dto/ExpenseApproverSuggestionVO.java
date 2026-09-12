package cn.aiedge.erp.finance.expensedoc.dto;

import lombok.Data;

/**
 * 费用审批人建议（自动指派结果）
 */
@Data
public class ExpenseApproverSuggestionVO {

    /** 审批级别 1-部门 2-财务 3-总经理 */
    private Integer level;

    /** 建议审批人ID（未配置/无法解析时为 null，需手选） */
    private Long approverId;

    /** 建议审批人 */
    private String approverName;

    /** 来源 DEPT_LEADER-部门负责人 CONFIG-审批人配置 NONE-未配置 */
    private String source;
}
