package cn.aiedge.workflow.engine;

import cn.aiedge.workflow.model.WorkflowDefinition;
import cn.aiedge.workflow.model.WorkflowInstance;
import cn.aiedge.workflow.model.WorkflowDefinition.WorkflowNode;
import cn.aiedge.workflow.model.WorkflowDefinition.ConditionBranch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工作流引擎单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@DisplayName("工作流引擎单元测试")
public class WorkflowEngineTest {

    private WorkflowEngineImpl workflowEngine;
    private StateTransitionManager stateManager;

    @BeforeEach
    void setUp() {
        workflowEngine = new WorkflowEngineImpl();
        stateManager = new StateTransitionManager();
    }

    // ==================== 节点执行测试 ====================

    @Test
    @DisplayName("执行开始节点 - 成功")
    void testExecuteStartNode() {
        // 准备数据
        WorkflowDefinition definition = createTestDefinition();
        WorkflowInstance instance = createTestInstance();
        WorkflowNode startNode = definition.getNodes().get(0);
        ExecutionContext context = ExecutionContext.builder()
                .instanceId(instance.getInstanceId())
                .build();

        // 执行测试
        WorkflowEngine.ExecutionResult result = workflowEngine.executeNode(
                instance, definition, startNode.getNodeId(), context);

        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals("completed", result.getStatus());
        assertNotNull(result.getNextNodeId());
    }

    @Test
    @DisplayName("执行审批节点 - 等待审批")
    void testExecuteApprovalNode() {
        // 准备数据
        WorkflowDefinition definition = createTestDefinition();
        WorkflowInstance instance = createTestInstance();
        WorkflowNode approvalNode = definition.getNodes().get(1);
        ExecutionContext context = ExecutionContext.builder()
                .instanceId(instance.getInstanceId())
                .approverId(1L)
                .build();

        // 执行测试
        WorkflowEngine.ExecutionResult result = workflowEngine.executeNode(
                instance, definition, approvalNode.getNodeId(), context);

        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals("pending", result.getStatus());
    }

    @Test
    @DisplayName("执行结束节点 - 成功")
    void testExecuteEndNode() {
        // 准备数据
        WorkflowDefinition definition = createTestDefinition();
        WorkflowInstance instance = createTestInstance();
        WorkflowNode endNode = definition.getNodes().get(2);
        ExecutionContext context = ExecutionContext.builder()
                .instanceId(instance.getInstanceId())
                .build();

        // 执行测试
        WorkflowEngine.ExecutionResult result = workflowEngine.executeNode(
                instance, definition, endNode.getNodeId(), context);

        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals("completed", result.getStatus());
    }

    @Test
    @DisplayName("执行条件节点 - 分支匹配")
    void testExecuteConditionNode() {
        // 准备数据
        WorkflowDefinition definition = createConditionalDefinition();
        WorkflowInstance instance = createTestInstance();
        WorkflowNode conditionNode = definition.getNodes().get(1);

        // 设置条件上下文
        Map<String, Object> businessData = new HashMap<>();
        businessData.put("amount", 5000);
        ExecutionContext context = ExecutionContext.builder()
                .instanceId(instance.getInstanceId())
                .businessData(businessData)
                .build();

        // 执行测试
        WorkflowEngine.ExecutionResult result = workflowEngine.executeNode(
                instance, definition, conditionNode.getNodeId(), context);

        // 验证结果
        assertTrue(result.isSuccess());
        assertNotNull(result.getNextNodeId());
    }

    // ==================== 条件评估测试 ====================

    @Test
    @DisplayName("条件评估 - 数值比较")
    void testEvaluateConditionNumber() {
        Map<String, Object> businessData = new HashMap<>();
        businessData.put("amount", 5000);

        ExecutionContext context = ExecutionContext.builder()
                .businessData(businessData)
                .build();

        // 测试大于
        boolean result = workflowEngine.evaluateCondition("#data['amount'] > 3000", context);
        assertTrue(result);

        // 测试小于
        result = workflowEngine.evaluateCondition("#data['amount'] < 3000", context);
        assertFalse(result);
    }

    @Test
    @DisplayName("条件评估 - 字符串匹配")
    void testEvaluateConditionString() {
        Map<String, Object> businessData = new HashMap<>();
        businessData.put("type", "urgent");

        ExecutionContext context = ExecutionContext.builder()
                .businessData(businessData)
                .build();

        boolean result = workflowEngine.evaluateCondition("#data['type'] == 'urgent'", context);
        assertTrue(result);
    }

    @Test
    @DisplayName("条件评估 - 复杂表达式")
    void testEvaluateConditionComplex() {
        Map<String, Object> businessData = new HashMap<>();
        businessData.put("amount", 5000);
        businessData.put("type", "urgent");

        ExecutionContext context = ExecutionContext.builder()
                .businessData(businessData)
                .build();

        // 测试 AND 条件
        boolean result = workflowEngine.evaluateCondition(
                "#data['amount'] > 3000 && #data['type'] == 'urgent'", context);
        assertTrue(result);
    }

    // ==================== 下一节点计算测试 ====================

    @Test
    @DisplayName("计算下一节点 - 顺序节点")
    void testCalculateNextNodeSequential() {
        WorkflowDefinition definition = createTestDefinition();
        WorkflowNode approvalNode = definition.getNodes().get(1);
        ExecutionContext context = new ExecutionContext();

        String nextNodeId = workflowEngine.calculateNextNode(definition, approvalNode.getNodeId(), context);

        assertNotNull(nextNodeId);
        assertEquals("end", nextNodeId);
    }

    @Test
    @DisplayName("计算下一节点 - 条件分支")
    void testCalculateNextNodeConditional() {
        WorkflowDefinition definition = createConditionalDefinition();
        WorkflowNode conditionNode = definition.getNodes().get(1);

        Map<String, Object> businessData = new HashMap<>();
        businessData.put("amount", 10000);

        ExecutionContext context = ExecutionContext.builder()
                .businessData(businessData)
                .build();

        String nextNodeId = workflowEngine.calculateNextNode(definition, conditionNode.getNodeId(), context);

        // 应该匹配到大额审批分支
        assertNotNull(nextNodeId);
    }

    // ==================== 状态流转测试 ====================

    @Test
    @DisplayName("状态转换 - 合法转换")
    void testValidTransition() {
        assertTrue(stateManager.isValidTransition("draft", "pending"));
        assertTrue(stateManager.isValidTransition("pending", "approving"));
        assertTrue(stateManager.isValidTransition("approving", "approved"));
        assertTrue(stateManager.isValidTransition("approving", "rejected"));
    }

    @Test
    @DisplayName("状态转换 - 非法转换")
    void testInvalidTransition() {
        assertFalse(stateManager.isValidTransition("completed", "approving"));
        assertFalse(stateManager.isValidTransition("cancelled", "approved"));
        assertFalse(stateManager.isValidTransition("draft", "approved"));
    }

    @Test
    @DisplayName("状态转换 - 执行转换")
    void testExecuteTransition() {
        WorkflowInstance instance = createTestInstance();
        instance.setStatus("approving");

        StateTransitionManager.TransitionResult result = stateManager.transition(
                instance, "approved", 1L, "审批通过");

        assertTrue(result.isSuccess());
        assertEquals("approving", result.getPreviousStatus());
        assertEquals("approved", result.getCurrentStatus());
    }

    @Test
    @DisplayName("状态转换 - 非法转换被拒绝")
    void testExecuteInvalidTransition() {
        WorkflowInstance instance = createTestInstance();
        instance.setStatus("completed");

        StateTransitionManager.TransitionResult result = stateManager.transition(
                instance, "approving", 1L, "非法转换");

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("获取允许的状态转换")
    void testGetAllowedTransitions() {
        List<String> transitions = stateManager.getAllowedTransitions("approving");

        assertTrue(transitions.contains("approved"));
        assertTrue(transitions.contains("rejected"));
        assertTrue(transitions.contains("cancelled"));
        assertTrue(transitions.contains("withdrawn"));
    }

    @Test
    @DisplayName("终态检查")
    void testIsFinalState() {
        assertTrue(stateManager.isFinalState("completed"));
        assertTrue(stateManager.isFinalState("cancelled"));
        assertFalse(stateManager.isFinalState("approving"));
        assertFalse(stateManager.isFinalState("approved"));
    }

    @Test
    @DisplayName("操作权限检查")
    void testOperationPermissions() {
        // 可以取消的状态
        assertTrue(stateManager.canCancel("draft"));
        assertTrue(stateManager.canCancel("pending"));
        assertTrue(stateManager.canCancel("approving"));

        // 可以撤回的状态
        assertTrue(stateManager.canWithdraw("approving"));

        // 终态不能操作
        assertFalse(stateManager.canCancel("completed"));
        assertFalse(stateManager.canWithdraw("completed"));
    }

    // ==================== 辅助方法 ====================

    private WorkflowDefinition createTestDefinition() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setDefinitionId("test_workflow");
        def.setName("测试工作流");
        def.setType("test");
        def.setVersion(1);

        List<WorkflowNode> nodes = new ArrayList<>();

        // 开始节点
        WorkflowNode start = new WorkflowNode();
        start.setNodeId("start");
        start.setNodeName("开始");
        start.setNodeType("start");
        start.setNextNodeId("approval");
        nodes.add(start);

        // 审批节点
        WorkflowNode approval = new WorkflowNode();
        approval.setNodeId("approval");
        approval.setNodeName("审批");
        approval.setNodeType("approval");
        approval.setApproverType("user");
        approval.setApproverIds(List.of("1", "2"));
        approval.setApproveMode("any");
        approval.setNextNodeId("end");
        nodes.add(approval);

        // 结束节点
        WorkflowNode end = new WorkflowNode();
        end.setNodeId("end");
        end.setNodeName("结束");
        end.setNodeType("end");
        nodes.add(end);

        def.setNodes(nodes);
        return def;
    }

    private WorkflowDefinition createConditionalDefinition() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setDefinitionId("conditional_workflow");
        def.setName("条件工作流");
        def.setType("test");
        def.setVersion(1);

        List<WorkflowNode> nodes = new ArrayList<>();

        // 开始节点
        WorkflowNode start = new WorkflowNode();
        start.setNodeId("start");
        start.setNodeName("开始");
        start.setNodeType("start");
        start.setNextNodeId("condition");
        nodes.add(start);

        // 条件节点
        WorkflowNode condition = new WorkflowNode();
        condition.setNodeId("condition");
        condition.setNodeName("金额判断");
        condition.setNodeType("condition");

        List<ConditionBranch> branches = new ArrayList<>();
        ConditionBranch branch1 = new ConditionBranch();
        branch1.setName("大额审批");
        branch1.setExpression("#data['amount'] > 10000");
        branch1.setTargetNodeId("manager_approval");
        branches.add(branch1);

        ConditionBranch branch2 = new ConditionBranch();
        branch2.setName("普通审批");
        branch2.setExpression("#data['amount'] <= 10000");
        branch2.setTargetNodeId("normal_approval");
        branches.add(branch2);

        condition.setBranches(branches);
        nodes.add(condition);

        // 大额审批节点
        WorkflowNode managerApproval = new WorkflowNode();
        managerApproval.setNodeId("manager_approval");
        managerApproval.setNodeName("经理审批");
        managerApproval.setNodeType("approval");
        managerApproval.setNextNodeId("end");
        nodes.add(managerApproval);

        // 普通审批节点
        WorkflowNode normalApproval = new WorkflowNode();
        normalApproval.setNodeId("normal_approval");
        normalApproval.setNodeName("普通审批");
        normalApproval.setNodeType("approval");
        normalApproval.setNextNodeId("end");
        nodes.add(normalApproval);

        // 结束节点
        WorkflowNode end = new WorkflowNode();
        end.setNodeId("end");
        end.setNodeName("结束");
        end.setNodeType("end");
        nodes.add(end);

        def.setNodes(nodes);
        return def;
    }

    private WorkflowInstance createTestInstance() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setInstanceId(UUID.randomUUID().toString());
        instance.setDefinitionId("test_workflow");
        instance.setWorkflowName("测试工作流");
        instance.setBusinessType("test");
        instance.setBusinessId("123");
        instance.setStatus("approving");
        instance.setApplicantId(1L);
        instance.setTenantId(1L);
        return instance;
    }
}
