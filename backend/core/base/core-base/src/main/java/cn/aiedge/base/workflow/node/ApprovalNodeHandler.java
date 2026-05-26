package cn.aiedge.base.workflow.node;

import cn.aiedge.base.workflow.model.ProcessInstance;
import cn.aiedge.base.workflow.model.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 审批节点处理器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class ApprovalNodeHandler implements NodeHandler {

    @Override
    public boolean handle(ProcessInstance instance, Task task, Map<String, Object> variables) {
        log.info("处理审批节点: instanceId={}, taskId={}, nodeId={}", 
            instance.getId(), task.getId(), task.getNodeId());
        
        // 审批节点处理逻辑：创建待办任务，等待审批人处理
        task.setState("PENDING");
        
        return false; // 等待人工审批，不自动流转
    }

    @Override
    public String getNodeType() {
        return "approval";
    }
}
