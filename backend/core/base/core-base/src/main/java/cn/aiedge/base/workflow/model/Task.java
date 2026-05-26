package cn.aiedge.base.workflow.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 任务模型
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class Task {
    
    private String id;
    private String processInstanceId;
    private String nodeId;
    private String nodeName;
    private String assigneeId;
    private String assigneeName;
    private String state;
    private String action;
    private String comment;
    private LocalDateTime createTime;
    private LocalDateTime completeTime;
}
