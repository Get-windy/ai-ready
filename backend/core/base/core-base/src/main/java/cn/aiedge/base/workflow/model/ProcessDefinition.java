package cn.aiedge.base.workflow.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 流程定义模型
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class ProcessDefinition {
    
    private String id;
    private String processKey;
    private String processName;
    private String description;
    private Integer version;
    private String status;
    private List<FlowNode> nodes;
    private List<FlowTransition> transitions;
    private Map<String, Object> variables;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
