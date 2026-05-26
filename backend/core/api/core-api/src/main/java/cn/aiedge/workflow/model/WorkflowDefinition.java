package cn.aiedge.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审批流程定义
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "审批流程定义")
public class WorkflowDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程定义ID
     */
    @Schema(description = "流程定义ID")
    private String definitionId;

    /**
     * 流程名称
     */
    @Schema(description = "流程名称")
    private String name;

    /**
     * 流程编码
     */
    @Schema(description = "流程编码")
    private String code;

    /**
     * 流程类型: order/leave/expense/reimbursement/purchase
     */
    @Schema(description = "流程类型")
    private String type;

    /**
     * 流程描述
     */
    @Schema(description = "流程描述")
    private String description;

    /**
     * 流程版本
     */
    @Schema(description = "流程版本")
    private int version;

    /**
     * 流程节点列表
     */
    @Schema(description = "流程节点列表")
    private List<WorkflowNode> nodes;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用")
    private boolean enabled;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // Getters and Setters
    public String getDefinitionId() { return definitionId; }
    public void setDefinitionId(String definitionId) { this.definitionId = definitionId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public List<WorkflowNode> getNodes() { return nodes; }
    public void setNodes(List<WorkflowNode> nodes) { this.nodes = nodes; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    /**
     * 流程节点
     */
    @Schema(description = "流程节点")
    public static class WorkflowNode implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        @Schema(description = "节点ID")
        private String nodeId;
        
        @Schema(description = "节点名称")
        private String nodeName;
        
        @Schema(description = "节点类型: start/approval/cc/condition/end")
        private String nodeType;
        
        @Schema(description = "审批人类型: user/role/dept/leader")
        private String approverType;
        
        @Schema(description = "审批人ID列表")
        private List<String> approverIds;
        
        @Schema(description = "审批方式: any/all/sequence")
        private String approveMode;
        
        @Schema(description = "超时时间（小时）")
        private int timeoutHours;
        
        @Schema(description = "超时处理: autoApprove/autoReject/notify")
        private String timeoutAction;
        
        @Schema(description = "下一节点ID")
        private String nextNodeId;
        
        @Schema(description = "条件表达式（条件节点）")
        private String conditionExpression;
        
        @Schema(description = "条件分支")
        private List<ConditionBranch> branches;

        // Getters and Setters
        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public String getNodeName() { return nodeName; }
        public void setNodeName(String nodeName) { this.nodeName = nodeName; }
        public String getNodeType() { return nodeType; }
        public void setNodeType(String nodeType) { this.nodeType = nodeType; }
        public String getApproverType() { return approverType; }
        public void setApproverType(String approverType) { this.approverType = approverType; }
        public List<String> getApproverIds() { return approverIds; }
        public void setApproverIds(List<String> approverIds) { this.approverIds = approverIds; }
        public String getApproveMode() { return approveMode; }
        public void setApproveMode(String approveMode) { this.approveMode = approveMode; }
        public int getTimeoutHours() { return timeoutHours; }
        public void setTimeoutHours(int timeoutHours) { this.timeoutHours = timeoutHours; }
        public String getTimeoutAction() { return timeoutAction; }
        public void setTimeoutAction(String timeoutAction) { this.timeoutAction = timeoutAction; }
        public String getNextNodeId() { return nextNodeId; }
        public void setNextNodeId(String nextNodeId) { this.nextNodeId = nextNodeId; }
        public String getConditionExpression() { return conditionExpression; }
        public void setConditionExpression(String conditionExpression) { this.conditionExpression = conditionExpression; }
        public List<ConditionBranch> getBranches() { return branches; }
        public void setBranches(List<ConditionBranch> branches) { this.branches = branches; }
    }

    /**
     * 条件分支
     */
    @Schema(description = "条件分支")
    public static class ConditionBranch implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        @Schema(description = "分支名称")
        private String name;
        
        @Schema(description = "条件表达式")
        private String expression;
        
        @Schema(description = "目标节点ID")
        private String targetNodeId;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        public String getTargetNodeId() { return targetNodeId; }
        public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }
    }
}
