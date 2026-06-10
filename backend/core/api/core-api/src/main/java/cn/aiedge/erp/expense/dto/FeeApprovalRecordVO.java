package cn.aiedge.erp.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批记录VO
 */
@Data
@Schema(description = "审批记录")
public class FeeApprovalRecordVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务单据ID")
    private Long businessId;

    @Schema(description = "审批级别")
    private Integer approvalLevel;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "审批人姓名")
    private String approverName;

    @Schema(description = "审批人部门")
    private String approverDepartmentName;

    @Schema(description = "审批动作")
    private String approvalAction;

    @Schema(description = "审批意见")
    private String approvalComment;

    @Schema(description = "审批时间")
    private LocalDateTime approvalTime;

    @Schema(description = "前状态")
    private String previousStatus;

    @Schema(description = "当前状态")
    private String currentStatus;
}
