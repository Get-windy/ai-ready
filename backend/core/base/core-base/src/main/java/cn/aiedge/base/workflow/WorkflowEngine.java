package cn.aiedge.base.workflow;

import cn.aiedge.base.workflow.model.ProcessDefinition;
import cn.aiedge.base.workflow.model.ProcessInstance;
import cn.aiedge.base.workflow.model.Task;

import java.util.List;
import java.util.Map;

/**
 * 工作流引擎核心接口
 * 提供流程定义、实例管理和任务处理的核心能力
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface WorkflowEngine {

    /**
     * 部署流程定义
     * 
     * @param definition 流程定义
     * @return 流程定义ID
     */
    String deployProcess(ProcessDefinition definition);

    /**
     * 启动流程实例
     * 
     * @param processDefinitionId 流程定义ID
     * @param businessKey 业务主键
     * @param variables 流程变量
     * @return 流程实例
     */
    ProcessInstance startProcess(String processDefinitionId, String businessKey, Map<String, Object> variables);

    /**
     * 完成任务
     * 
     * @param taskId 任务ID
     * @param action 审批动作（approve/reject/transfer）
     * @param comment 审批意见
     * @param variables 流程变量
     */
    void completeTask(String taskId, String action, String comment, Map<String, Object> variables);

    /**
     * 获取待办任务列表
     * 
     * @param userId 用户ID
     * @return 任务列表
     */
    List<Task> getTodoTasks(String userId);

    /**
     * 获取流程实例
     * 
     * @param processInstanceId 流程实例ID
     * @return 流程实例
     */
    ProcessInstance getProcessInstance(String processInstanceId);

    /**
     * 终止流程实例
     * 
     * @param processInstanceId 流程实例ID
     * @param reason 终止原因
     */
    void terminateProcess(String processInstanceId, String reason);

    /**
     * 获取流程历史
     * 
     * @param processInstanceId 流程实例ID
     * @return 历史任务列表
     */
    List<Task> getProcessHistory(String processInstanceId);
}
