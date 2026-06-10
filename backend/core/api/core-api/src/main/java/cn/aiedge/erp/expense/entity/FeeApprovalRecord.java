package cn.aiedge.erp.expense.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批记录表
 */
@Data
@TableName("fee_approval_record")
public class FeeApprovalRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 业务类型: APPLICATION/REIMBURSEMENT */
    private String businessType;

    /** 业务单据ID */
    private Long businessId;

    /** 审批级别 */
    private Integer approvalLevel;

    /** 审批人ID */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;

    /** 审批人部门ID */
    private Long approverDepartmentId;

    /** 审批人部门名称 */
    private String approverDepartmentName;

    /** 审批动作: SUBMIT/APPROVE/REJECT/RETURN/TRANSFER/WITHDRAW/CANCEL */
    private String approvalAction;

    /** 审批意见 */
    private String approvalComment;

    /** 审批时间 */
    private LocalDateTime approvalTime;

    /** 转交人ID */
    private Long assigneeId;

    /** 转交人姓名 */
    private String assigneeName;

    /** 前状态 */
    private String previousStatus;

    /** 当前状态 */
    private String currentStatus;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;
}
