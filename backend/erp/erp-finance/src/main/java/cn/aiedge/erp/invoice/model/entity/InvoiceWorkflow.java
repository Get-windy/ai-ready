package cn.aiedge.erp.invoice.model.entity;

import cn.aiedge.erp.invoice.model.enums.ApprovalAction;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 发票审批工作流实体类
 * 记录发票申请的审批流程历史
 */
@Entity
@Table(name = "invoice_workflow")
@Data
@EqualsAndHashCode(callSuper = false)
public class InvoiceWorkflow extends BaseEntity {
    
    /**
     * 关联发票申请
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private InvoiceApplication invoiceApplication;
    
    /**
     * 审批步骤
     */
    @Column(name = "step", nullable = false)
    private Integer step;
    
    /**
     * 审批节点名称
     */
    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;
    
    /**
     * 审批人ID
     */
    @Column(name = "approver_id", nullable = false)
    private Long approverId;
    
    /**
     * 审批人姓名
     */
    @Column(name = "approver_name", length = 100)
    private String approverName;
    
    /**
     * 审批人部门
     */
    @Column(name = "approver_department", length = 100)
    private String approverDepartment;
    
    /**
     * 审批角色
     */
    @Column(name = "approver_role", length = 50)
    private String approverRole;
    
    /**
     * 审批动作
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private ApprovalAction action;
    
    /**
     * 审批意见
     */
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;
    
    /**
     * 审批时间
     */
    @Column(name = "approved_at", nullable = false)
    private LocalDateTime approvedAt;
    
    /**
     * 是否为系统自动审批
     */
    @Column(name = "is_auto")
    private Boolean isAuto = false;
    
    /**
     * 审批耗时（毫秒）
     */
    @Column(name = "duration_ms")
    private Long durationMs;
    
    /**
     * 下一审批人ID
     */
    @Column(name = "next_approver_id")
    private Long nextApproverId;
    
    /**
     * 下一审批人姓名
     */
    @Column(name = "next_approver_name", length = 100)
    private String nextApproverName;
    
    /**
     * 获取审批动作显示名称
     */
    public String getActionDisplay() {
        return action != null ? action.getChineseName() : "未知";
    }
    
    @Override
    public boolean validate() {
        return step != null && stepName != null && approverId != null && action != null && approvedAt != null;
    }
    
    @Override
    public String getEntityType() {
        return "INVOICE_WORKFLOW";
    }
    
    @Override
    public String getDisplayName() {
        return String.format("Workflow[%d]-%s", step, stepName);
    }
}