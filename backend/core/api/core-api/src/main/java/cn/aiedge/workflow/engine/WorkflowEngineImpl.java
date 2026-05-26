package cn.aiedge.workflow.engine;

import cn.aiedge.workflow.model.WorkflowDefinition;
import cn.aiedge.workflow.model.WorkflowInstance;
import cn.aiedge.workflow.model.WorkflowDefinition.WorkflowNode;
import cn.aiedge.workflow.model.WorkflowDefinition.ConditionBranch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 工作流执行引擎实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEngineImpl implements WorkflowEngine {

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Override
    public ExecutionResult executeNode(WorkflowInstance instance, WorkflowDefinition definition,
                                        String nodeId, ExecutionContext context) {
        // 查找节点
        WorkflowNode node = findNode(definition, nodeId);
        if (node == null) {
            return ExecutionResult.failure("节点不存在: " + nodeId);
        }

        log.info("执行节点: instanceId={}, nodeId={}, nodeType={}", 
                instance.getInstanceId(), nodeId, node.getNodeType());

        // 根据节点类型执行
        return switch (node.getNodeType()) {
            case "start" -> executeStartNode(instance, node, context);
            case "approval" -> executeApprovalNode(instance, node, context);
            case "condition" -> executeConditionNode(instance, node, context);
            case "cc" -> executeCcNode(instance, node, context);
            case "end" -> executeEndNode(instance, node, context);
            default -> ExecutionResult.failure("未知的节点类型: " + node.getNodeType());
        };
    }

    @Override
    public void triggerEvent(String instanceId, String eventType, Map<String, Object> eventData) {
        log.info("触发流程事件: instanceId={}, eventType={}", instanceId, eventType);
        
        // 根据事件类型处理
        switch (eventType) {
            case "timeout" -> handleTimeoutEvent(instanceId, eventData);
            case "remind" -> handleRemindEvent(instanceId, eventData);
            case "cancel" -> handleCancelEvent(instanceId, eventData);
            default -> log.debug("未处理的事件类型: {}", eventType);
        }
    }

    @Override
    public boolean evaluateCondition(String expression, ExecutionContext context) {
        if (expression == null || expression.isEmpty()) {
            return true;
        }

        try {
            StandardEvaluationContext evalContext = new StandardEvaluationContext();
            
            // 设置变量上下文
            if (context.getBusinessData() != null) {
                evalContext.setVariable("data", context.getBusinessData());
            }
            if (context.getVariables() != null) {
                context.getVariables().forEach(evalContext::setVariable);
            }

            Boolean result = expressionParser.parseExpression(expression)
                    .getValue(evalContext, Boolean.class);
            
            log.debug("条件评估: expression={}, result={}", expression, result);
            return Boolean.TRUE.equals(result);
            
        } catch (Exception e) {
            log.error("条件表达式评估失败: {}", expression, e);
            return false;
        }
    }

    @Override
    public String calculateNextNode(WorkflowDefinition definition, String currentNodeId, 
                                     ExecutionContext context) {
        WorkflowNode currentNode = findNode(definition, currentNodeId);
        if (currentNode == null) {
            return null;
        }

        // 条件节点：评估分支
        if ("condition".equals(currentNode.getNodeType())) {
            return evaluateConditionBranch(currentNode, context);
        }

        // 其他节点：直接返回下一节点
        return currentNode.getNextNodeId();
    }

    // ==================== 节点执行方法 ====================

    private ExecutionResult executeStartNode(WorkflowInstance instance, WorkflowNode node, 
                                             ExecutionContext context) {
        log.info("执行开始节点: instanceId={}", instance.getInstanceId());
        return ExecutionResult.success(node.getNextNodeId());
    }

    private ExecutionResult executeApprovalNode(WorkflowInstance instance, WorkflowNode node,
                                                 ExecutionContext context) {
        log.info("执行审批节点: instanceId={}, nodeName={}", instance.getInstanceId(), node.getNodeName());
        
        // 检查审批人
        List<String> approverIds = node.getApproverIds();
        if (approverIds == null || approverIds.isEmpty()) {
            log.warn("审批节点没有配置审批人: {}", node.getNodeId());
            // 自动通过
            return ExecutionResult.success(node.getNextNodeId());
        }

        // 设置当前审批人信息到上下文
        context.setVariable("approverType", node.getApproverType());
        context.setVariable("approveMode", node.getApproveMode());
        context.setVariable("timeoutHours", node.getTimeoutHours());

        // 返回等待审批
        return ExecutionResult.pending("等待审批: " + node.getNodeName());
    }

    private ExecutionResult executeConditionNode(WorkflowInstance instance, WorkflowNode node,
                                                  ExecutionContext context) {
        log.info("执行条件节点: instanceId={}", instance.getInstanceId());
        
        String nextNodeId = evaluateConditionBranch(node, context);
        if (nextNodeId == null) {
            log.warn("条件节点没有匹配的分支: {}", node.getNodeId());
            return ExecutionResult.failure("条件分支不匹配");
        }
        
        return ExecutionResult.success(nextNodeId);
    }

    private ExecutionResult executeCcNode(WorkflowInstance instance, WorkflowNode node,
                                           ExecutionContext context) {
        log.info("执行抄送节点: instanceId={}", instance.getInstanceId());
        
        // 抄送节点自动通过
        // 实际应发送通知给抄送人
        return ExecutionResult.success(node.getNextNodeId());
    }

    private ExecutionResult executeEndNode(WorkflowInstance instance, WorkflowNode node,
                                            ExecutionContext context) {
        log.info("执行结束节点: instanceId={}", instance.getInstanceId());
        return ExecutionResult.builder()
                .success(true)
                .status("completed")
                .message("流程结束")
                .build();
    }

    // ==================== 事件处理方法 ====================

    private void handleTimeoutEvent(String instanceId, Map<String, Object> eventData) {
        log.warn("处理超时事件: instanceId={}", instanceId);
        // 实际应更新实例状态或执行超时动作
    }

    private void handleRemindEvent(String instanceId, Map<String, Object> eventData) {
        log.info("处理提醒事件: instanceId={}", instanceId);
        // 实际应发送提醒通知
    }

    private void handleCancelEvent(String instanceId, Map<String, Object> eventData) {
        log.info("处理取消事件: instanceId={}", instanceId);
        // 实际应取消流程
    }

    // ==================== 辅助方法 ====================

    private WorkflowNode findNode(WorkflowDefinition definition, String nodeId) {
        if (definition.getNodes() == null) {
            return null;
        }
        return definition.getNodes().stream()
                .filter(n -> nodeId.equals(n.getNodeId()))
                .findFirst()
                .orElse(null);
    }

    private String evaluateConditionBranch(WorkflowNode conditionNode, ExecutionContext context) {
        List<ConditionBranch> branches = conditionNode.getBranches();
        if (branches == null || branches.isEmpty()) {
            return conditionNode.getNextNodeId();
        }

        // 按顺序评估每个分支
        for (ConditionBranch branch : branches) {
            if (evaluateCondition(branch.getExpression(), context)) {
                log.debug("条件分支匹配: branch={}, target={}", branch.getName(), branch.getTargetNodeId());
                return branch.getTargetNodeId();
            }
        }

        // 没有匹配的分支，返回默认下一节点
        return conditionNode.getNextNodeId();
    }
}
