package cn.aiedge.base.workflow.node;

import cn.aiedge.base.workflow.model.ProcessInstance;
import cn.aiedge.base.workflow.model.Task;
import cn.aiedge.base.workflow.statemachine.ProcessStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 结束节点处理器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class EndNodeHandler implements NodeHandler {

    @Override
    public boolean handle(ProcessInstance instance, Task task, Map<String, Object> variables) {
        log.info("处理结束节点: instanceId={}", instance.getId());
        
        // 结束节点处理：完成任务，结束流程
        task.setState("COMPLETED");
        instance.setState(ProcessStateMachine.InstanceState.COMPLETED.name());
        instance.setEndTime(LocalDateTime.now());
        
        return false; // 流程结束
    }

    @Override
    public String getNodeType() {
        return "end";
    }
}
