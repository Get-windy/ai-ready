package cn.aiedge.erp.expense.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用报销单表
 */
@Data
@TableName("fee_reimbursement")
public class FeeReimbursement {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 报销单号 */
    private String reimbursementNo;

    /** 报销标题 */
    private String reimbursementTitle;

    /** 报销人ID */
    private Long applicantId;

    /** 报销人姓名 */
    private String applicantName;

    /** 部门ID */
    private Long departmentId;

    /** 部门名称 */
    private String departmentName;

    /** 关联费用申请ID */
    private Long applicationId;

    /** 关联申请单号 */
    private String applicationNo;

    /** 报销总金额 */
    private BigDecimal totalAmount;

    /** 币种 */
    private String currency;

    /** 报销日期 */
    private LocalDate reimbursementDate;

    /** 报销事由 */
    private String purpose;

    /** 详细说明 */
    private String description;

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

    /** 支付方式: CASH/BANK_TRANSFER/CHECK/ALIPAY/WECHAT/CREDIT_CARD/DEBIT_CARD/OTHER */
    private String paymentMethod;

    /** 支付账户 */
    private String paymentAccount;

    /** 付款状态: NONE/PENDING/COMPLETED */
    private String paymentStatus;

    /** 已付款金额 */
    private BigDecimal paidAmount;

    /** 付款日期 */
    private LocalDate paymentDate;

    /** 付款凭证号 */
    private String paymentVoucherNo;

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
