package cn.aiedge.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批记录
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "审批记录")
public class ApprovalRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @Schema(description = "记录ID")
    private Long recordId;

    /**
     * 流程实例ID
     */
    @Schema(description = "流程实例ID")
    private String instanceId;

    /**
     * 节点ID
     */
    @Schema(description = "节点ID")
    private String nodeId;

    /**
     * 节点名称
     */
    @Schema(description = "节点名称")
    private String nodeName;

    /**
     * 审批人ID
     */
    @Schema(description = "审批人ID")
    private Long approverId;

    /**
     * 审批人名称
     */
    @Schema(description = "审批人名称")
    private String approverName;

    /**
     * 审批动作: approve/reject/withdraw/transfer
     */
    @Schema(description = "审批动作")
    private String action;

    /**
     * 审批意见
     */
    @Schema(description = "审批意见")
    private String comment;

    /**
     * 审批时间
     */
    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    /**
     * 状态: pending/approved/rejected
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 附件URL
     */
    @Schema(description = "附件URL")
    private String attachmentUrl;

    // Getters and Setters
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }
    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getApproveTime() { return approveTime; }
    public void setApproveTime(LocalDateTime approveTime) { this.approveTime = approveTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
}
