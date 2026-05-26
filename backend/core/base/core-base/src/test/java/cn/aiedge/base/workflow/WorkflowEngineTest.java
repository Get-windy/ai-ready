package cn.aiedge.base.workflow;

import cn.aiedge.base.workflow.model.ProcessDefinition;
import cn.aiedge.base.workflow.model.ProcessInstance;
import cn.aiedge.base.workflow.statemachine.ProcessStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工作流引擎单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
class WorkflowEngineTest {

    private ProcessStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new ProcessStateMachine();
    }

    @Test
    void testInstanceStateTransition_RunningToCompleted() {
        ProcessInstance instance = new ProcessInstance();
        instance.setState(ProcessStateMachine.InstanceState.RUNNING.name());
        
        stateMachine.transitionInstance(instance, ProcessStateMachine.InstanceState.COMPLETED);
        
        assertEquals(ProcessStateMachine.InstanceState.COMPLETED.name(), instance.getState());
    }

    @Test
    void testInstanceStateTransition_RunningToTerminated() {
        ProcessInstance instance = new ProcessInstance();
        instance.setState(ProcessStateMachine.InstanceState.RUNNING.name());
        
        stateMachine.transitionInstance(instance, ProcessStateMachine.InstanceState.TERMINATED);
        
        assertEquals(ProcessStateMachine.InstanceState.TERMINATED.name(), instance.getState());
    }

    @Test
    void testInstanceStateTransition_InvalidTransition() {
        ProcessInstance instance = new ProcessInstance();
        instance.setState(ProcessStateMachine.InstanceState.COMPLETED.name());
        
        assertThrows(IllegalStateException.class, () -> {
            stateMachine.transitionInstance(instance, ProcessStateMachine.InstanceState.RUNNING);
        });
    }

    @Test
    void testTaskStateTransition_Approve() {
        cn.aiedge.base.workflow.model.Task task = new cn.aiedge.base.workflow.model.Task();
        task.setState(ProcessStateMachine.TaskState.PENDING.name());
        
        ProcessStateMachine.TaskState newState = stateMachine.transitionTask(
            task, ProcessStateMachine.Action.APPROVE);
        
        assertEquals(ProcessStateMachine.TaskState.COMPLETED, newState);
        assertEquals(ProcessStateMachine.TaskState.COMPLETED.name(), task.getState());
    }

    @Test
    void testTaskStateTransition_Reject() {
        cn.aiedge.base.workflow.model.Task task = new cn.aiedge.base.workflow.model.Task();
        task.setState(ProcessStateMachine.TaskState.PENDING.name());
        
        ProcessStateMachine.TaskState newState = stateMachine.transitionTask(
            task, ProcessStateMachine.Action.REJECT);
        
        assertEquals(ProcessStateMachine.TaskState.COMPLETED, newState);
    }

    @Test
    void testTaskStateTransition_Transfer() {
        cn.aiedge.base.workflow.model.Task task = new cn.aiedge.base.workflow.model.Task();
        task.setState(ProcessStateMachine.TaskState.PENDING.name());
        
        ProcessStateMachine.TaskState newState = stateMachine.transitionTask(
            task, ProcessStateMachine.Action.TRANSFER);
        
        assertEquals(ProcessStateMachine.TaskState.TRANSFERRED, newState);
    }

    @Test
    void testProcessDefinitionModel() {
        ProcessDefinition definition = new ProcessDefinition();
        definition.setId("def-001");
        definition.setProcessKey("leave-approval");
        definition.setProcessName("请假审批");
        definition.setVersion(1);
        definition.setStatus("ACTIVE");
        
        assertEquals("def-001", definition.getId());
        assertEquals("leave-approval", definition.getProcessKey());
        assertEquals("请假审批", definition.getProcessName());
    }

    @Test
    void testProcessInstanceModel() {
        ProcessInstance instance = new ProcessInstance();
        instance.setId("inst-001");
        instance.setProcessDefinitionId("def-001");
        instance.setBusinessKey("LEAVE-2024-001");
        instance.setState("RUNNING");
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("days", 3);
        variables.put("reason", "病假");
        instance.setVariables(variables);
        
        assertEquals("inst-001", instance.getId());
        assertEquals("RUNNING", instance.getState());
        assertNotNull(instance.getVariables());
        assertEquals(3, instance.getVariables().get("days"));
    }
}
