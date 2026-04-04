package cn.aiedge.workflow.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流执行上下文
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class ExecutionContext {

    /**
     * 流程实例ID
     */
    private String instanceId;

    /**
     * 流程定义ID
     */
    private String definitionId;

    /**
     * 当前节点ID
     */
    private String currentNodeId;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 业务数据
     */
    private Map<String, Object> businessData;

    /**
     * 流程变量
     */
    private Map<String, Object> variables;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 租户ID
     */
    private Long tenantId;

    public ExecutionContext() {
        this.variables = new HashMap<>();
        this.businessData = new HashMap<>();
    }

    /**
     * 设置变量
     */
    public void setVariable(String key, Object value) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        variables.put(key, value);
    }

    /**
     * 获取变量
     */
    public Object getVariable(String key) {
        return variables != null ? variables.get(key) : null;
    }

    /**
     * 获取字符串变量
     */
    public String getStringVariable(String key) {
        Object value = getVariable(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取数值变量
     */
    public Long getLongVariable(String key) {
        Object value = getVariable(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    /**
     * 设置业务数据
     */
    public void setBusinessData(String key, Object value) {
        if (businessData == null) {
            businessData = new HashMap<>();
        }
        businessData.put(key, value);
    }

    /**
     * 获取业务数据
     */
    public Object getBusinessData(String key) {
        return businessData != null ? businessData.get(key) : null;
    }

    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构建器
     */
    public static class Builder {
        private final ExecutionContext context = new ExecutionContext();

        public Builder instanceId(String instanceId) {
            context.setInstanceId(instanceId);
            return this;
        }

        public Builder definitionId(String definitionId) {
            context.setDefinitionId(definitionId);
            return this;
        }

        public Builder currentNodeId(String currentNodeId) {
            context.setCurrentNodeId(currentNodeId);
            return this;
        }

        public Builder businessType(String businessType) {
            context.setBusinessType(businessType);
            return this;
        }

        public Builder businessId(String businessId) {
            context.setBusinessId(businessId);
            return this;
        }

        public Builder businessData(Map<String, Object> businessData) {
            context.setBusinessData(businessData);
            return this;
        }

        public Builder variables(Map<String, Object> variables) {
            context.setVariables(variables);
            return this;
        }

        public Builder applicantId(Long applicantId) {
            context.setApplicantId(applicantId);
            return this;
        }

        public Builder approverId(Long approverId) {
            context.setApproverId(approverId);
            return this;
        }

        public Builder comment(String comment) {
            context.setComment(comment);
            return this;
        }

        public Builder tenantId(Long tenantId) {
            context.setTenantId(tenantId);
            return this;
        }

        public ExecutionContext build() {
            return context;
        }
    }

    // Getters and Setters
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public String getDefinitionId() { return definitionId; }
    public void setDefinitionId(String definitionId) { this.definitionId = definitionId; }
    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }
    public Map<String, Object> getBusinessData() { return businessData; }
    public void setBusinessData(Map<String, Object> businessData) { this.businessData = businessData; }
    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
