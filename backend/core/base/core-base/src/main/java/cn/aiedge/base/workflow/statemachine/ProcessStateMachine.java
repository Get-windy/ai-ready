package cn.aiedge.base.workflow.statemachine;

import cn.aiedge.base.workflow.model.ProcessInstance;
import cn.aiedge.base.workflow.model.Task;

/**
 * 流程状态机
 * 管理流程实例和任务的状态流转
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class ProcessStateMachine {

    /**
     * 流程实例状态
     */
    public enum InstanceState {
        RUNNING("运行中"),
        COMPLETED("已完成"),
        TERMINATED("已终止"),
        SUSPENDED("已挂起");

        private final String description;

        InstanceState(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 任务状态
     */
    public enum TaskState {
        PENDING("待处理"),
        PROCESSING("处理中"),
        COMPLETED("已完成"),
        TRANSFERRED("已转交"),
        RETURNED("已退回");

        private final String description;

        TaskState(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 审批动作
     */
    public enum Action {
        APPROVE("同意"),
        REJECT("驳回"),
        TRANSFER("转交"),
        RETURN("退回");

        private final String description;

        Action(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 流转流程实例状态
     * 
     * @param instance 流程实例
     * @param targetState 目标状态
     */
    public void transitionInstance(ProcessInstance instance, InstanceState targetState) {
        InstanceState currentState = InstanceState.valueOf(instance.getState());
        
        // 验证状态流转是否合法
        if (!isValidInstanceTransition(currentState, targetState)) {
            throw new IllegalStateException(
                String.format("非法的状态流转: %s -> %s", currentState, targetState));
        }
        
        instance.setState(targetState.name());
    }

    /**
     * 流转任务状态
     * 
     * @param task 任务
     * @param action 审批动作
     * @return 新状态
     */
    public TaskState transitionTask(Task task, Action action) {
        TaskState currentState = TaskState.valueOf(task.getState());
        TaskState newState = calculateNewTaskState(currentState, action);
        
        task.setState(newState.name());
        return newState;
    }

    /**
     * 验证流程实例状态流转是否合法
     */
    private boolean isValidInstanceTransition(InstanceState from, InstanceState to) {
        return switch (from) {
            case RUNNING -> to == InstanceState.COMPLETED || 
                            to == InstanceState.TERMINATED || 
                            to == InstanceState.SUSPENDED;
            case SUSPENDED -> to == InstanceState.RUNNING || 
                              to == InstanceState.TERMINATED;
            case COMPLETED, TERMINATED -> false; // 终态不可流转
        };
    }

    /**
     * 计算任务新状态
     */
    private TaskState calculateNewTaskState(TaskState currentState, Action action) {
        return switch (action) {
            case APPROVE -> TaskState.COMPLETED;
            case REJECT -> TaskState.COMPLETED;
            case TRANSFER -> TaskState.TRANSFERRED;
            case RETURN -> TaskState.RETURNED;
        };
    }
}
