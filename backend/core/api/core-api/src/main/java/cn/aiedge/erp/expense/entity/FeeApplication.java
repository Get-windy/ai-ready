package cn.aiedge.erp.expense.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用申请表
 */
@Data
@TableName("fee_application")
public class FeeApplication {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 申请单号 */
    private String applicationNo;

    /** 申请标题 */
    private String applicationTitle;

    /** 申请人ID */
    private Long applicantId;

    /** 申请人姓名 */
    private String applicantName;

    /** 部门ID */
    private Long departmentId;

    /** 部门名称 */
    private String departmentName;

    /** 费用类型: TRAVEL, OFFICE_SUPPLIES, MEETING, etc. */
    private String expenseType;

    /** 费用类型描述 */
    private String expenseTypeDesc;

    /** 费用总金额 */
    private BigDecimal totalAmount;

    /** 币种 */
    private String currency;

    /** 预算金额 */
    private BigDecimal budgetAmount;

    /** 已使用预算 */
    private BigDecimal budgetUsed;

    /** 预算使用率(%) */
    private BigDecimal budgetUsageRate;

    /** 是否超出预算(0=否 1=是) */
    private Integer exceedBudget;

    /** 超出金额 */
    private BigDecimal exceedAmount;

    /** 超出原因 */
    private String exceedReason;

    /** 申请日期 */
    private LocalDate applyDate;

    /** 费用事由 */
    private String purpose;

    /** 详细说明 */
    private String description;

    /** 是否紧急(0=否 1=是) */
    private Integer isUrgent;

    /** 紧急原因 */
    private String urgentReason;

    /** 预计完成日期 */
    private LocalDate expectedCompletionDate;

    /** 实际完成日期 */
    private LocalDate actualCompletionDate;

    /** 附件数量 */
    private Integer attachmentCount;

    /** 状态: DRAFT/SUBMITTED/APPROVING/APPROVED/REJECTED/CANCELLED/WITHDRAWN */
    private String status;

    /** 当前审批人ID */
    private Long currentApproverId;

    /** 当前审批人姓名 */
    private String currentApproverName;

    /** 当前审批级别 */
    private Integer currentApprovalLevel;

    /** 总审批级别数 */
    private Integer totalApprovalLevel;

    /** 审批意见 */
    private String approvalComment;

    /** 拒绝原因 */
    private String rejectReason;

    /** 取消原因 */
    private String cancelReason;

    /** 工作流实例ID */
    private String processInstanceId;

    /** 工作流定义ID */
    private String processDefinitionId;

    /** 报销状态: NONE/PARTIAL/COMPLETED */
    private String reimbursementStatus;

    /** 已报销金额 */
    private BigDecimal reimbursedAmount;

    /** 报销日期 */
    private LocalDate reimbursementDate;

    /** 付款状态: NONE/PENDING/PARTIAL/COMPLETED */
    private String paymentStatus;

    /** 已付款金额 */
    private BigDecimal paidAmount;

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
