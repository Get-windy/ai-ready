package cn.aiedge.erp.finance.expensedoc.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批处理入参（通过/驳回）
 */
@Data
public class ApprovalProcessDTO {

    /** 费用单ID */
    @NotNull(message = "费用单ID不能为空")
    private Long docId;

    /** 审批动作 APPROVE-通过 REJECT-驳回 */
    @NotNull(message = "审批动作不能为空")
    private String action;

    /** 审批意见（通过选填）/ 驳回原因（驳回必填） */
    private String comment;

    /** 下一级审批人（未到末级时必填） */
    private Long nextApproverId;

    private String nextApproverName;
}
