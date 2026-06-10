package cn.aiedge.erp.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批请求
 */
@Data
@Schema(description = "审批请求")
public class FeeApprovalRequest {

    @Schema(description = "业务类型: APPLICATION/REIMBURSEMENT")
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @Schema(description = "业务单据ID")
    @NotNull(message = "业务单据ID不能为空")
    private Long businessId;

    @Schema(description = "审批动作: APPROVE/REJECT/RETURN/TRANSFER")
    @NotBlank(message = "审批动作不能为空")
    private String action;

    @Schema(description = "审批意见")
    private String comment;

    @Schema(description = "转交人ID(转交时必填)")
    private Long assigneeId;

    @Schema(description = "转交人姓名")
    private String assigneeName;
}
