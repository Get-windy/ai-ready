package cn.aiedge.workflow.engine;

import cn.aiedge.workflow.model.WorkflowInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 状态流转管理器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StateTransitionManager {

    /**
     * 状态转移规则
     */
    private static final Map<String, Set<String>> TRANSITIONS = new LinkedHashMap<>();

    static {
        // 初始化状态转移规则
        TRANSITIONS.put("draft", new HashSet<>(Arrays.asList("pending", "cancelled")));
        TRANSITIONS.put("pending", new HashSet<>(Arrays.asList("approving", "cancelled")));
        TRANSITIONS.put("approving", new HashSet<>(Arrays.asList("approved", "rejected", "cancelled", "withdrawn")));
        TRANSITIONS.put("approved", new HashSet<>(Arrays.asList("completed", "cancelled")));
        TRANSITIONS.put("rejected", new HashSet<>(Arrays.asList("resubmit", "cancelled")));
        TRANSITIONS.put("withdrawn", new HashSet<>(Arrays.asList("resubmit", "cancelled")));
        TRANSITIONS.put("completed", Collections.emptySet());
        TRANSITIONS.put("cancelled", Collections.emptySet());
    }

    /**
     * 检查状态转换是否合法
     *
     * @param currentStatus 当前状态
     * @param targetStatus  目标状态
     * @return 是否合法
     */
    public boolean isValidTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }

        // 相同状态不需要转换
        if (currentStatus.equals(targetStatus)) {
            return true;
        }

        Set<String> allowedTargets = TRANSITIONS.get(currentStatus);
        return allowedTargets != null && allowedTargets.contains(targetStatus);
    }

    /**
     * 获取允许的下一状态列表
     *
     * @param currentStatus 当前状态
     * @return 允许的状态列表
     */
    public List<String> getAllowedTransitions(String currentStatus) {
        Set<String> allowed = TRANSITIONS.get(currentStatus);
        return allowed != null ? new ArrayList<>(allowed) : Collections.emptyList();
    }

    /**
     * 执行状态转换
     *
     * @param instance      流程实例
     * @param targetStatus  目标状态
     * @param operatorId    操作人ID
     * @param reason        原因
     * @return 转换结果
     */
    public TransitionResult transition(WorkflowInstance instance, String targetStatus, 
                                        Long operatorId, String reason) {
        String currentStatus = instance.getStatus();

        // 验证状态转换
        if (!isValidTransition(currentStatus, targetStatus)) {
            log.warn("非法状态转换: instanceId={}, from={}, to={}", 
                    instance.getInstanceId(), currentStatus, targetStatus);
            return TransitionResult.failure("非法状态转换: " + currentStatus + " -> " + targetStatus);
        }

        // 执行状态转换
        String previousStatus = currentStatus;
        instance.setStatus(targetStatus);

        // 设置完成时间
        if (isFinalState(targetStatus)) {
            instance.setCompleteTime(LocalDateTime.now());
        }

        log.info("状态转换成功: instanceId={}, from={}, to={}, operator={}", 
                instance.getInstanceId(), previousStatus, targetStatus, operatorId);

        return TransitionResult.success(previousStatus, targetStatus);
    }

    /**
     * 检查是否是终态
     *
     * @param status 状态
     * @return 是否是终态
     */
    public boolean isFinalState(String status) {
        return "completed".equals(status) || "cancelled".equals(status);
    }

    /**
     * 检查是否可以取消
     *
     * @param status 当前状态
     * @return 是否可以取消
     */
    public boolean canCancel(String status) {
        return isValidTransition(status, "cancelled");
    }

    /**
     * 检查是否可以撤回
     *
     * @param status 当前状态
     * @return 是否可以撤回
     */
    public boolean canWithdraw(String status) {
        return isValidTransition(status, "withdrawn");
    }

    /**
     * 检查是否可以重新提交
     *
     * @param status 当前状态
     * @return 是否可以重新提交
     */
    public boolean canResubmit(String status) {
        return isValidTransition(status, "resubmit");
    }

    /**
     * 获取状态描述
     *
     * @param status 状态
     * @return 描述
     */
    public String getStatusDescription(String status) {
        return switch (status) {
            case "draft" -> "草稿";
            case "pending" -> "待提交";
            case "approving" -> "审批中";
            case "approved" -> "已通过";
            case "rejected" -> "已拒绝";
            case "cancelled" -> "已取消";
            case "withdrawn" -> "已撤回";
            case "completed" -> "已完成";
            case "resubmit" -> "重新提交";
            default -> status;
        };
    }

    /**
     * 状态转换结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TransitionResult {
        private boolean success;
        private String previousStatus;
        private String currentStatus;
        private String errorMessage;
        private LocalDateTime transitionTime;

        public static TransitionResult success(String previous, String current) {
            return TransitionResult.builder()
                    .success(true)
                    .previousStatus(previous)
                    .currentStatus(current)
                    .transitionTime(LocalDateTime.now())
                    .build();
        }

        public static TransitionResult failure(String errorMessage) {
            return TransitionResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        }
    }
}
