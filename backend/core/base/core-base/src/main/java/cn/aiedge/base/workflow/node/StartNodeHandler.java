package cn.aiedge.base.workflow.node;

import cn.aiedge.base.workflow.model.ProcessInstance;
import cn.aiedge.base.workflow.model.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 开始节点处理器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class StartNodeHandler implements NodeHandler {

    @Override
    public boolean handle(ProcessInstance instance, Task task, Map<String, Object> variables) {
        log.info("处理开始节点: instanceId={}", instance.getId());
        
        // 开始节点自动完成，继续流转
        task.setState("COMPLETED");
        
        return true; // 自动流转到下一个节点
    }

    @Override
    public String getNodeType() {
        return "start";
    }
}
