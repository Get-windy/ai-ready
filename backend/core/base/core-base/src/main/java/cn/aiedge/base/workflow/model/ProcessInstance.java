package cn.aiedge.base.workflow.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 流程实例模型
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class ProcessInstance {
    
    private String id;
    private String processDefinitionId;
    private String businessKey;
    private String state;
    private String currentNodeId;
    private String startUserId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Map<String, Object> variables;
}
