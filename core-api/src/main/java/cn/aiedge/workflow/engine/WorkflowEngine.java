package cn.aiedge.workflow.engine;

import cn.aiedge.workflow.model.WorkflowDefinition;
import cn.aiedge.workflow.model.WorkflowInstance;

import java.util.Map;

/**
 * 工作流执行引擎接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface WorkflowEngine {

    /**
     * 执行流程节点
     *
     * @param instance    流程实例
     * @param definition  流程定义
     * @param nodeId      节点ID
     * @param context     执行上下文
     * @return 执行结果
     */
    ExecutionResult executeNode(WorkflowInstance instance, WorkflowDefinition definition, 
                                String nodeId, ExecutionContext context);

    /**
     * 触发流程事件
     *
     * @param instanceId 实例ID
     * @param eventType  事件类型
     * @param eventData  事件数据
     */
    void triggerEvent(String instanceId, String eventType, Map<String, Object> eventData);

    /**
     * 评估条件表达式
     *
     * @param expression 条件表达式
     * @param context    执行上下文
     * @return 评估结果
     */
    boolean evaluateCondition(String expression, ExecutionContext context);

    /**
     * 计算下一个节点
     *
     * @param definition    流程定义
     * @param currentNodeId 当前节点ID
     * @param context       执行上下文
     * @return 下一个节点ID
     */
    String calculateNextNode(WorkflowDefinition definition, String currentNodeId, ExecutionContext context);

    /**
     * 执行结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class ExecutionResult {
        /**
         * 是否成功
         */
        private boolean success;

        /**
         * 结果状态
         */
        private String status;

        /**
         * 下一个节点ID
         */
        private String nextNodeId;

        /**
         * 消息
         */
        private String message;

        /**
         * 错误信息
         */
        private String errorMessage;

        public static ExecutionResult success(String nextNodeId) {
            return ExecutionResult.builder()
                    .success(true)
                    .status("completed")
                    .nextNodeId(nextNodeId)
                    .build();
        }

        public static ExecutionResult pending(String message) {
            return ExecutionResult.builder()
                    .success(true)
                    .status("pending")
                    .message(message)
                    .build();
        }

        public static ExecutionResult failure(String errorMessage) {
            return ExecutionResult.builder()
                    .success(false)
                    .status("failed")
                    .errorMessage(errorMessage)
                    .build();
        }
    }
}
