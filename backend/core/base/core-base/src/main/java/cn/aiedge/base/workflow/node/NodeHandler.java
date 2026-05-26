package cn.aiedge.base.workflow.node;

import cn.aiedge.base.workflow.model.ProcessInstance;
import cn.aiedge.base.workflow.model.Task;

import java.util.Map;

/**
 * 流程节点处理器接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface NodeHandler {
    
    /**
     * 处理节点
     * 
     * @param instance 流程实例
     * @param task 当前任务
     * @param variables 流程变量
     * @return 是否继续流转
     */
    boolean handle(ProcessInstance instance, Task task, Map<String, Object> variables);
    
    /**
     * 获取节点类型
     */
    String getNodeType();
}
