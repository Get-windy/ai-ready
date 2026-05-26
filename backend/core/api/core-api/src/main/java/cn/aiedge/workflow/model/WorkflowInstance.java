package cn.aiedge.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审批流程实例
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "审批流程实例")
public class WorkflowInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    @Schema(description = "实例ID")
    private String instanceId;

    /**
     * 流程定义ID
     */
    @Schema(description = "流程定义ID")
    private String definitionId;

    /**
     * 流程名称
     */
    @Schema(description = "流程名称")
    private String workflowName;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型")
    private String businessType;

    /**
     * 业务ID
     */
    @Schema(description = "业务ID")
    private String businessId;

    /**
     * 业务数据
     */
    @Schema(description = "业务数据")
    private Map<String, Object> businessData;

    /**
     * 当前节点ID
     */
    @Schema(description = "当前节点ID")
    private String currentNodeId;

    /**
     * 当前节点名称
     */
    @Schema(description = "当前节点名称")
    private String currentNodeName;

    /**
     * 状态: pending/approving/approved/rejected/cancelled
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 申请人ID
     */
    @Schema(description = "申请人ID")
    private Long applicantId;

    /**
     * 申请人名称
     */
    @Schema(description = "申请人名称")
    private String applicantName;

    /**
     * 申请时间
     */
    @Schema(description = "申请时间")
    private LocalDateTime applyTime;

    /**
     * 完成时间
     */
    @Schema(description = "完成时间")
    private LocalDateTime completeTime;

    /**
     * 租户ID
     */
    @Schema(description = "租户ID")
    private Long tenantId;

    // Getters and Setters
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public String getDefinitionId() { return definitionId; }
    public void setDefinitionId(String definitionId) { this.definitionId = definitionId; }
    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }
    public Map<String, Object> getBusinessData() { return businessData; }
    public void setBusinessData(Map<String, Object> businessData) { this.businessData = businessData; }
    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }
    public String getCurrentNodeName() { return currentNodeName; }
    public void setCurrentNodeName(String currentNodeName) { this.currentNodeName = currentNodeName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public LocalDateTime getApplyTime() { return applyTime; }
    public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }
    public LocalDateTime getCompleteTime() { return completeTime; }
    public void setCompleteTime(LocalDateTime completeTime) { this.completeTime = completeTime; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
