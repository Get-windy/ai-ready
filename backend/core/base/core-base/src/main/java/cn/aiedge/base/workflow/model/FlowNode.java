package cn.aiedge.base.workflow.model;

import lombok.Data;
import java.util.Map;

/**
 * 流程节点模型
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class FlowNode {
    
    private String id;
    private String nodeKey;
    private String nodeName;
    private String nodeType;
    private String assigneeType;
    private String assigneeId;
    private Integer sort;
    private String conditionExpression;
    private Map<String, Object> properties;
}
