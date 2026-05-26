package cn.aiedge.erp.price.engine.execution;

import cn.aiedge.erp.price.engine.execution.dto.ExecutionRequest;
import cn.aiedge.erp.price.engine.execution.dto.ExecutionResult;

import java.util.List;

/**
 * 价格策略执行器接口
 */
public interface IPriceStrategyExecutor {
    
    /**
     * 执行策略
     * @param request 执行请求
     * @return 执行结果
     */
    ExecutionResult execute(ExecutionRequest request);
    
    /**
     * 批量执行
     * @param requests 执行请求列表
     * @return 执行结果列表
     */
    List<ExecutionResult> executeBatch(List<ExecutionRequest> requests);
    
    /**
     * 预执行检查
     * @param request 执行请求
     * @return 预检结果
     */
    PreExecutionCheck preCheck(ExecutionRequest request);
    
    /**
     * 取消执行
     * @param executionId 执行ID
     * @return 取消结果
     */
    CancelResult cancelExecution(String executionId);
    
    /**
     * 暂停执行
     * @param executionId 执行ID
     * @return 暂停结果
     */
    PauseResult pauseExecution(String executionId);
    
    /**
     * 恢复执行
     * @param executionId 执行ID
     * @return 恢复结果
     */
    ResumeResult resumeExecution(String executionId);
    
    /**
     * 获取执行状态
     * @param executionId 执行ID
     * @return 执行状态
     */
    ExecutionStatus getStatus(String executionId);
    
    /**
     * 获取执行历史
     * @param strategyId 策略ID
     * @param limit 限制数量
     * @return 执行历史列表
     */
    List<ExecutionHistory> getExecutionHistory(String strategyId, int limit);
    
    /**
     * 清理完成的执行
     * @param olderThanDays 天数
     * @return 清理数量
     */
    int cleanupExecutions(int olderThanDays);
    
    /**
     * 执行器统计信息
     * @return 统计信息
     */
    ExecutorStats getStats();
    
    /**
     * 健康检查
     * @return 健康状态
     */
    HealthStatus healthCheck();
    
    /**
     * 预执行检查结果
     */
    record PreExecutionCheck(
            boolean canExecute,
            List<CheckItem> checks,
            List<Dependency> missingDependencies,
            List<Risk> identifiedRisks,
            String recommendation
    ) {
        public record CheckItem(
                String checkId,
                String description,
                boolean passed,
                String details,
                CheckSeverity severity
        ) {}
        
        public enum CheckSeverity {
            CRITICAL,
            HIGH,
            MEDIUM,
            LOW
        }
    }
    
    /**
     * 执行状态
     */
    record ExecutionStatus(
            String executionId,
            Status currentStatus,
            double progressPercentage,
            LocalDateTime startTime,
            LocalDateTime estimatedCompletion,
            List<StatusTransition> statusHistory,
            Map<String, Object> currentState
    ) {
        public enum Status {
            PENDING,            // 待执行
            PRE_CHECKING,       // 预检中
            EXECUTING,          // 执行中
            PAUSED,             // 已暂停
            COMPLETED,          // 已完成
            FAILED,             // 已失败
            CANCELLED           // 已取消
        }
        
        public record StatusTransition(
                Status fromStatus,
                Status toStatus,
                LocalDateTime transitionTime,
                String reason,
                String actor
        ) {}
    }
    
    /**
     * 取消结果
     */
    record CancelResult(
            boolean success,
            String executionId,
            String message,
            List<RollbackAction> rollbackActions,
            LocalDateTime cancelledAt
    ) {}
    
    /**
     * 暂停结果
     */
    record PauseResult(
            boolean success,
            String executionId,
            String message,
            LocalDateTime pausedAt,
            String resumeToken
    ) {}
    
    /**
     * 恢复结果
     */
    record ResumeResult(
            boolean success,
            String executionId,
            String message,
            LocalDateTime resumedAt
    ) {}
    
    /**
     * 执行历史
     */
    record ExecutionHistory(
            String executionId,
            String strategyId,
            ExecutionResult result,
            LocalDateTime executionTime,
            long durationMs,
            String executorId
    ) {}
    
    /**
     * 执行器统计
     */
    record ExecutorStats(
            long totalExecutions,
            long successfulExecutions,
            long failedExecutions,
            long cancelledExecutions,
            double averageExecutionTimeMs,
            double successRate,
            Map<String, Long> executionByType,
            Map<String, Long> executionByPriority
    ) {}
    
    /**
     * 健康状态
     */
    record HealthStatus(
            boolean healthy,
            String status,
            List<ComponentHealth> components,
            LocalDateTime checkedAt
    ) {
        public record ComponentHealth(
                String componentName,
                boolean healthy,
                String status,
                String details,
                LocalDateTime lastChecked
        ) {}
    }
    
    /**
     * 依赖
     */
    record Dependency(
            String dependencyId,
            String description,
            DependencyType type,
            String resourceId,
            boolean available
    ) {
        public enum DependencyType {
            DATABASE,
            EXTERNAL_SERVICE,
            FILE_SYSTEM,
            MESSAGE_QUEUE,
            CACHE
        }
    }
    
    /**
     * 风险
     */
    record Risk(
            String riskId,
            String description,
            RiskLevel level,
            double probability,
            String impact,
            List<Mitigation> mitigations
    ) {
        public enum RiskLevel {
            CRITICAL,
            HIGH,
            MEDIUM,
            LOW
        }
        
        public record Mitigation(
                String action,
                String responsibility,
                LocalDateTime deadline
        ) {}
    }
    
    /**
     * 回滚动作
     */
    record RollbackAction(
            String actionId,
            String description,
            RollbackType type,
            Map<String, Object> parameters,
            boolean executed
    ) {
        public enum RollbackType {
            DATABASE_ROLLBACK,
            FILE_RESTORE,
            MESSAGE_SEND,
            API_CALL,
            NOTIFICATION
        }
    }
}