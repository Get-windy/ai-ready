package cn.aiedge.erp.finance.expensedoc.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交审批入参
 */
@Data
public class ApprovalSubmitDTO {

    /** 费用单ID */
    @NotNull(message = "费用单ID不能为空")
    private Long docId;

    /** 审批总级数（1-3，默认 3：部门→财务→总经理） */
    private Integer totalLevel;

    /** 第一级审批人 */
    private Long approverId;

    private String approverName;
}
