package cn.aiedge.erp.finance.expensedoc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 费用审批记录实体
 * 每次审批操作（提交/通过/驳回）留痕一条，可经 /records 追溯。
 * 审批状态本身落在 erp_expense_doc（复用费用单状态机，不另建审批主表）。
 */
@Data
@Accessors(chain = true)
@TableName("erp_expense_approval")
public class ExpenseApproval {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 费用单ID */
    private Long expenseDocId;

    /** 费用单号 YBFYD- */
    private String docNo;

    /** 本次审批级别 1-部门 2-财务 3-总经理 */
    private Integer approvalLevel;

    /** 审批人ID */
    private Long approverId;

    /** 审批人 */
    private String approverName;

    /** 审批动作 SUBMIT-提交 APPROVE-通过 REJECT-驳回 */
    private String approvalAction;

    /** 审批意见/驳回原因 */
    private String approvalComment;

    /** 审批时间 */
    private LocalDateTime approvalTime;

    /** 审批前状态（含级别描述） */
    private String previousStatus;

    /** 审批后状态（含级别描述） */
    private String currentStatus;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer versionNo;
}
