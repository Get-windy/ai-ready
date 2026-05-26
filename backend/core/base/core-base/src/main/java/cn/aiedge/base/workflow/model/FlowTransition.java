package cn.aiedge.base.workflow.model;

import lombok.Data;

/**
 * 流程流转模型
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class FlowTransition {
    
    private String id;
    private String sourceNodeId;
    private String targetNodeId;
    private String conditionExpression;
    private String description;
}
