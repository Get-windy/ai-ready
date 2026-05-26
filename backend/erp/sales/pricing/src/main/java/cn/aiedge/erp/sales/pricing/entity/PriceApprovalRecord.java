package cn.aiedge.erp.sales.pricing.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 价格审批记录实体类
 * 记录价格特批申请的详细审批历史
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "price_approval_record")
public class PriceApprovalRecord {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 审批记录编号
     */
    @Column(name = "record_code", nullable = false, unique = true, length = 64)
    private String recordCode;
    
    /**
     * 价格特批申请ID
     */
    @Column(name = "approval_id", nullable = false)
    private Long approvalId;
    
    /**
     * 审批操作类型
     */
    @Column(name = "action_type", nullable = false, length = 32)
    private String actionType;
    
    /**
     * 操作描述
     */
    @Column(name = "action_description", length = 255)
    private String actionDescription;
    
    /**
     * 审批级别（1-4）
     */
    @Column(name = "approval_level")
    private Integer approvalLevel;
    
    /**
     * 操作前的审批状态
     */
    @Column(name = "previous_status", length = 32)
    private String previousStatus;
    
    /**
     * 操作后的审批状态
     */
    @Column(name = "current_status", length = 32)
    private String currentStatus;
    
    /**
     * 操作人ID
     */
    @Column(name = "operator_id", nullable = false)
    private Long operatorId;
    
    /**
     * 操作人姓名
     */
    @Column(name = "operator_name", nullable = false, length = 128)
    private String operatorName;
    
    /**
     * 操作人角色
     */
    @Column(name = "operator_role", length = 64)
    private String operatorRole;
    
    /**
     * 操作人部门ID
     */
    @Column(name = "operator_department_id")
    private Long operatorDepartmentId;
    
    /**
     * 操作人部门名称
     */
    @Column(name = "operator_department_name", length = 128)
    private String operatorDepartmentName;
    
    /**
     * 操作意见
     */
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;
    
    /**
     * 审批决定（通过/拒绝/撤回等）
     */
    @Column(name = "decision", length = 32)
    private String decision;
    
    /**
     * 拒绝原因（如果审批被拒绝）
     */
    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;
    
    /**
     * 建议修改内容
     */
    @Column(name = "suggestions", columnDefinition = "TEXT")
    private String suggestions;
    
    /**
     * 附件数量
     */
    @Column(name = "attachment_count")
    private Integer attachmentCount;
    
    /**
     * 操作IP地址
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * 操作设备信息
     */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;
    
    /**
     * 操作地理位置
     */
    @Column(name = "location", length = 255)
    private String location;
    
    /**
     * 是否自动操作
     */
    @Column(name = "is_auto_action")
    private Boolean isAutoAction;
    
    /**
     * 操作耗时（毫秒）
     */
    @Column(name = "operation_duration_ms")
    private Long operationDurationMs;
    
    /**
     * 操作是否成功
     */
    @Column(name = "is_success")
    private Boolean isSuccess;
    
    /**
     * 错误信息（如果操作失败）
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * 租户ID
     */
    @Column(name = "tenant_id")
    private Long tenantId;
    
    /**
     * 是否删除（0:未删除，1:已删除）
     */
    @Column(name = "deleted")
    private Integer deleted;
    
    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 创建人ID
     */
    @Column(name = "create_by")
    private Long createBy;
    
    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
    
    /**
     * 更新人ID
     */
    @Column(name = "update_by")
    private Long updateBy;
    
    // ========== 静态常量 ==========
    
    /**
     * 操作类型常量
     */
    public static class ActionType {
        public static final String CREATE = "CREATE";           // 创建申请
        public static final String SUBMIT = "SUBMIT";           // 提交申请
        public static final String APPROVE = "APPROVE";         // 审批通过
        public static final String REJECT = "REJECT";           // 审批拒绝
        public static final String WITHDRAW = "WITHDRAW";       // 撤回申请
        public static final String CANCEL = "CANCEL";           // 取消申请
        public static final String EXPIRED = "EXPIRED";         // 申请过期
        public static final String MODIFY = "MODIFY";           // 修改申请
        public static final String REASSIGN = "REASSIGN";       // 重新分配审批人
        public static final String ESCALATE = "ESCALATE";       // 升级审批
        public static final String DELEGATE = "DELEGATE";       // 委托审批
        public static final String NOTIFY = "NOTIFY";           // 发送通知
        public static final String COMMENT = "COMMENT";         // 添加备注
        public static final String ATTACH = "ATTACH";           // 添加附件
        public static final String VIEW = "VIEW";               // 查看申请
        public static final String EXPORT = "EXPORT";           // 导出申请
        public static final String REMIND = "REMIND";           // 发送提醒
        public static final String SYSTEM = "SYSTEM";           // 系统操作
    }
    
    /**
     * 审批决定常量
     */
    public static class Decision {
        public static final String APPROVED = "APPROVED";       // 批准
        public static final String REJECTED = "REJECTED";       // 拒绝
        public static final String PENDING = "PENDING";         // 待定
        public static final String CONDITIONAL = "CONDITIONAL"; // 有条件批准
        public static final String RETURNED = "RETURNED";       // 退回修改
        public static final String AUTOMATIC = "AUTOMATIC";     // 自动审批
    }
    
    /**
     * 操作人角色常量
     */
    public static class OperatorRole {
        public static final String APPLICANT = "APPLICANT";           // 申请人
        public static final String APPROVER = "APPROVER";             // 审批人
        public static final String DEPARTMENT_MANAGER = "DEPARTMENT_MANAGER"; // 部门经理
        public static final String FINANCE_MANAGER = "FINANCE_MANAGER";       // 财务经理
        public static final String SALES_MANAGER = "SALES_MANAGER";           // 销售经理
        public static final String GENERAL_MANAGER = "GENERAL_MANAGER";       // 总经理
        public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";             // 系统管理员
        public static final String AUDITOR = "AUDITOR";                       // 审计员
    }
    
    // ========== 业务方法 ==========
    
    /**
     * 判断是否为审批操作
     */
    @Transient
    public boolean isApprovalAction() {
        return ActionType.APPROVE.equals(actionType) || ActionType.REJECT.equals(actionType);
    }
    
    /**
     * 判断是否为创建操作
     */
    @Transient
    public boolean isCreateAction() {
        return ActionType.CREATE.equals(actionType);
    }
    
    /**
     * 判断是否为提交操作
     */
    @Transient
    public boolean isSubmitAction() {
        return ActionType.SUBMIT.equals(actionType);
    }
    
    /**
     * 判断是否为撤回操作
     */
    @Transient
    public boolean isWithdrawAction() {
        return ActionType.WITHDRAW.equals(actionType);
    }
    
    /**
     * 判断是否为取消操作
     */
    @Transient
    public boolean isCancelAction() {
        return ActionType.CANCEL.equals(actionType);
    }
    
    /**
     * 判断是否为修改操作
     */
    @Transient
    public boolean isModifyAction() {
        return ActionType.MODIFY.equals(actionType);
    }
    
    /**
     * 判断是否为通知操作
     */
    @Transient
    public boolean isNotifyAction() {
        return ActionType.NOTIFY.equals(actionType);
    }
    
    /**
     * 判断是否审批通过
     */
    @Transient
    public boolean isApproved() {
        return Decision.APPROVED.equals(decision) || Decision.AUTOMATIC.equals(decision);
    }
    
    /**
     * 判断是否审批拒绝
     */
    @Transient
    public boolean isRejected() {
        return Decision.REJECTED.equals(decision);
    }
    
    /**
     * 判断是否为最终审批
     */
    @Transient
    public boolean isFinalApproval() {
        return approvalLevel != null && approvalLevel == 4 && isApprovalAction();
    }
    
    /**
     * 判断是否为系统操作
     */
    @Transient
    public boolean isSystemAction() {
        return ActionType.SYSTEM.equals(actionType) || 
               ActionType.EXPIRED.equals(actionType) || 
               Boolean.TRUE.equals(isAutoAction);
    }
    
    /**
     * 获取操作简要描述
     */
    @Transient
    public String getBriefDescription() {
        if (actionDescription != null && actionDescription.length() > 50) {
            return actionDescription.substring(0, 47) + "...";
        }
        return actionDescription;
    }
    
    /**
     * 判断是否有附件
     */
    @Transient
    public boolean hasAttachments() {
        return attachmentCount != null && attachmentCount > 0;
    }
    
    /**
     * 判断是否为申请人操作
     */
    @Transient
    public boolean isApplicantAction() {
        return OperatorRole.APPLICANT.equals(operatorRole);
    }
    
    /**
     * 判断是否为审批人操作
     */
    @Transient
    public boolean isApproverAction() {
        return OperatorRole.APPROVER.equals(operatorRole) ||
               OperatorRole.DEPARTMENT_MANAGER.equals(operatorRole) ||
               OperatorRole.FINANCE_MANAGER.equals(operatorRole) ||
               OperatorRole.SALES_MANAGER.equals(operatorRole) ||
               OperatorRole.GENERAL_MANAGER.equals(operatorRole);
    }
}