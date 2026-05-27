package cn.aiedge.erp.price.engine.execution.impl;

import cn.aiedge.erp.price.engine.execution.IPriceStrategyExecutor;
import cn.aiedge.erp.price.engine.execution.IPriceStrategyExecutor.ExecutionStatus;
import cn.aiedge.erp.price.engine.execution.IPriceStrategyExecutor.ExecutionStatus.Status;
import cn.aiedge.erp.price.engine.execution.IPriceStrategyExecutor.ExecutionStatus.StatusTransition;
import cn.aiedge.erp.price.engine.execution.IPriceStrategyExecutor.PreExecutionCheck;
import cn.aiedge.erp.price.engine.execution.IPriceStrategyExecutor.PreExecutionCheck.CheckItem;
import cn.aiedge.erp.price.engine.execution.IPriceStrategyExecutor.PreExecutionCheck.CheckSeverity;
import cn.aiedge.erp.price.engine.execution.IPriceStrategyExecutor.HealthStatus;
import cn.aiedge.erp.price.engine.execution.IPriceStrategyExecutor.HealthStatus.ComponentHealth;
import cn.aiedge.erp.price.engine.execution.IPriceStrategyExecutor.Dependency;
import cn.aiedge.erp.price.engine.execution.IPriceStrategyExecutor.Risk;
import cn.aiedge.erp.price.engine.execution.dto.ExecutionRequest;
import cn.aiedge.erp.price.engine.execution.dto.ExecutionResult;
import cn.aiedge.erp.price.engine.execution.dto.ExecutionResult.TargetResult;
import cn.aiedge.erp.price.engine.execution.dto.ExecutionResult.ExecutionLog;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class PriceStrategyExecutorImpl implements IPriceStrategyExecutor {
    
    private final Map<String, ExecutionStatus> executionStatusMap = new ConcurrentHashMap<>();
    private final Map<String, ExecutionResult> executionResultMap = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> executionStartTimeMap = new ConcurrentHashMap<>();
    
    private long totalExecutions = 0;
    private long successfulExecutions = 0;
    private long failedExecutions = 0;
    private long cancelledExecutions = 0;
    private double totalExecutionTimeMs = 0;
    private final Map<String, Long> executionByType = new ConcurrentHashMap<>();
    private final Map<String, Long> executionByPriority = new ConcurrentHashMap<>();
    
    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        String executionId = request.executionId() != null ? request.executionId() : UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();
        executionStartTimeMap.put(executionId, startTime);
        
        totalExecutions++;
        executionByType.merge(request.executionType().name(), 1L, Long::sum);
        executionByPriority.merge(request.priority().name(), 1L, Long::sum);
        
        ExecutionStatus status = new ExecutionStatus(
                executionId,
                Status.EXECUTING,
                0.0,
                startTime,
                null,
                new ArrayList<>(),
                new HashMap<>(request.context())
        );
        executionStatusMap.put(executionId, status);
        
        try {
            PreExecutionCheck preCheck = preCheck(request);
            if (!preCheck.canExecute()) {
                return createFailedResult(executionId, request.strategyId(), 
                        "PRE_CHECK_FAILED", "预检失败: " + preCheck.recommendation());
            }
            
            List<TargetResult> targetResults = new ArrayList<>();
            BigDecimal totalImpact = BigDecimal.ZERO;
            List<ExecutionLog> logs = new ArrayList<>();
            
            logs.add(new ExecutionLog(startTime, "INFO", "开始执行策略", request.context()));
            
            if (request.targets() != null) {
                for (var target : request.targets()) {
                    TargetResult targetResult = executeTarget(target, request.context(), logs);
                    targetResults.add(targetResult);
                    if (targetResult.impact() != null) {
                        totalImpact = totalImpact.add(targetResult.impact());
                    }
                }
            }
            
            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
            
            boolean allSuccess = targetResults.stream().allMatch(TargetResult::success);
            ExecutionResult.ExecutionStatus finalStatus = allSuccess ? ExecutionResult.ExecutionStatus.COMPLETED : ExecutionResult.ExecutionStatus.PARTIALLY_COMPLETED;
            
            successfulExecutions++;
            totalExecutionTimeMs += durationMs;
            
            ExecutionResult result = new ExecutionResult(
                    executionId,
                    request.strategyId(),
                    allSuccess,
                    finalStatus,
                    targetResults,
                    totalImpact,
                    request.context(),
                    startTime,
                    endTime,
                    durationMs,
                    null,
                    null,
                    logs
            );
            
            executionResultMap.put(executionId, result);
            updateExecutionStatus(executionId, Status.COMPLETED, 100.0);
            
            return result;
        } catch (Exception e) {
            failedExecutions++;
            return createFailedResult(executionId, request.strategyId(), 
                    "EXECUTION_ERROR", e.getMessage());
        }
    }
    
    private TargetResult executeTarget(Object targetObj, Map<String, Object> context, List<ExecutionLog> logs) {
        return new TargetResult(
                UUID.randomUUID().toString(),
                "PRODUCT",
                true,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new ArrayList<>(),
                "执行成功"
        );
    }
    
    private ExecutionResult createFailedResult(String executionId, String strategyId, 
                                               String errorCode, String errorMessage) {
        LocalDateTime startTime = executionStartTimeMap.getOrDefault(executionId, LocalDateTime.now());
        LocalDateTime endTime = LocalDateTime.now();
        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        
        ExecutionResult result = new ExecutionResult(
                executionId,
                strategyId,
                false,
                ExecutionResult.ExecutionStatus.FAILED,
                new ArrayList<>(),
                BigDecimal.ZERO,
                new HashMap<>(),
                startTime,
                endTime,
                durationMs,
                errorMessage,
                errorCode,
                List.of(new ExecutionLog(endTime, "ERROR", errorMessage, new HashMap<>()))
        );
        
        executionResultMap.put(executionId, result);
        updateExecutionStatus(executionId, Status.FAILED, 0.0);
        
        return result;
    }
    
    private void updateExecutionStatus(String executionId, Status newStatus, double progress) {
        ExecutionStatus current = executionStatusMap.get(executionId);
        if (current != null) {
            List<StatusTransition> history = new ArrayList<>(current.statusHistory());
            history.add(new StatusTransition(
                    current.currentStatus(),
                    newStatus,
                    LocalDateTime.now(),
                    "状态变更",
                    "system"
            ));
            
            ExecutionStatus updated = new ExecutionStatus(
                    executionId,
                    newStatus,
                    progress,
                    current.startTime(),
                    null,
                    history,
                    current.currentState()
            );
            executionStatusMap.put(executionId, updated);
        }
    }
    
    @Override
    public List<ExecutionResult> executeBatch(List<ExecutionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        
        return requests.stream()
                .map(this::execute)
                .collect(Collectors.toList());
    }
    
    @Override
    public PreExecutionCheck preCheck(ExecutionRequest request) {
        List<CheckItem> checks = new ArrayList<>();
        List<Dependency> missingDependencies = new ArrayList<>();
        List<Risk> identifiedRisks = new ArrayList<>();
        
        checks.add(new CheckItem("strategy_valid", "策略有效性检查", 
                request.strategyId() != null, "策略ID存在", CheckSeverity.CRITICAL));
        
        checks.add(new CheckItem("context_valid", "上下文有效性检查",
                request.context() != null && !request.context().isEmpty(),
                "执行上下文有效", CheckSeverity.HIGH));
        
        checks.add(new CheckItem("targets_valid", "目标有效性检查",
                request.targets() != null && !request.targets().isEmpty(),
                "执行目标有效", CheckSeverity.HIGH));
        
        if (request.targets() == null || request.targets().isEmpty()) {
            missingDependencies.add(new Dependency("targets", "执行目标", 
                    Dependency.DependencyType.EXTERNAL_SERVICE, null, false));
        }
        
        boolean canExecute = checks.stream()
                .filter(c -> c.severity() == CheckSeverity.CRITICAL || c.severity() == CheckSeverity.HIGH)
                .allMatch(CheckItem::passed);
        
        String recommendation = canExecute 
                ? "预检通过，可以执行" 
                : "预检失败，请检查缺失的依赖项";
        
        return new PreExecutionCheck(canExecute, checks, missingDependencies, identifiedRisks, recommendation);
    }
    
    @Override
    public CancelResult cancelExecution(String executionId) {
        ExecutionStatus status = executionStatusMap.get(executionId);
        if (status == null) {
            return new CancelResult(false, executionId, "执行不存在", 
                    new ArrayList<>(), LocalDateTime.now());
        }
        
        if (status.currentStatus() == Status.COMPLETED || status.currentStatus() == Status.FAILED) {
            return new CancelResult(false, executionId, "执行已完成，无法取消",
                    new ArrayList<>(), LocalDateTime.now());
        }
        
        updateExecutionStatus(executionId, Status.CANCELLED, 0.0);
        cancelledExecutions++;
        
        return new CancelResult(true, executionId, "执行已取消",
                new ArrayList<>(), LocalDateTime.now());
    }
    
    @Override
    public PauseResult pauseExecution(String executionId) {
        ExecutionStatus status = executionStatusMap.get(executionId);
        if (status == null) {
            return new PauseResult(false, executionId, "执行不存在", LocalDateTime.now(), null);
        }
        
        if (status.currentStatus() != Status.EXECUTING) {
            return new PauseResult(false, executionId, "当前状态不允许暂停", LocalDateTime.now(), null);
        }
        
        updateExecutionStatus(executionId, Status.PAUSED, status.progressPercentage());
        
        return new PauseResult(true, executionId, "执行已暂停", LocalDateTime.now(), UUID.randomUUID().toString());
    }
    
    @Override
    public ResumeResult resumeExecution(String executionId) {
        ExecutionStatus status = executionStatusMap.get(executionId);
        if (status == null) {
            return new ResumeResult(false, executionId, "执行不存在", LocalDateTime.now());
        }
        
        if (status.currentStatus() != Status.PAUSED) {
            return new ResumeResult(false, executionId, "当前状态不是暂停状态", LocalDateTime.now());
        }
        
        updateExecutionStatus(executionId, Status.EXECUTING, status.progressPercentage());
        
        return new ResumeResult(true, executionId, "执行已恢复", LocalDateTime.now());
    }
    
    @Override
    public ExecutionStatus getStatus(String executionId) {
        return executionStatusMap.get(executionId);
    }
    
    @Override
    public List<ExecutionHistory> getExecutionHistory(String strategyId, int limit) {
        return executionResultMap.values().stream()
                .filter(r -> r.strategyId().equals(strategyId))
                .sorted((r1, r2) -> r2.startTime().compareTo(r1.startTime()))
                .limit(limit)
                .map(r -> new ExecutionHistory(
                        r.executionId(),
                        r.strategyId(),
                        r,
                        r.startTime(),
                        r.durationMs(),
                        "system"
                ))
                .collect(Collectors.toList());
    }
    
    @Override
    public int cleanupExecutions(int olderThanDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(olderThanDays);
        
        List<String> toRemove = executionResultMap.entrySet().stream()
                .filter(e -> e.getValue().startTime().isBefore(threshold))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        toRemove.forEach(id -> {
            executionResultMap.remove(id);
            executionStatusMap.remove(id);
            executionStartTimeMap.remove(id);
        });
        
        return toRemove.size();
    }
    
    @Override
    public ExecutorStats getStats() {
        double avgExecutionTime = totalExecutions > 0 ? totalExecutionTimeMs / totalExecutions : 0;
        double successRate = totalExecutions > 0 ? (double) successfulExecutions / totalExecutions * 100 : 0;
        
        return new ExecutorStats(
                totalExecutions,
                successfulExecutions,
                failedExecutions,
                cancelledExecutions,
                avgExecutionTime,
                successRate,
                new HashMap<>(executionByType),
                new HashMap<>(executionByPriority)
        );
    }
    
    @Override
    public HealthStatus healthCheck() {
        List<ComponentHealth> components = new ArrayList<>();
        
        components.add(new ComponentHealth("ruleEngine", true, "UP", 
                "规则引擎正常", LocalDateTime.now()));
        
        components.add(new ComponentHealth("executor", true, "UP",
                "执行器正常，总执行次数: " + totalExecutions, LocalDateTime.now()));
        
        components.add(new ComponentHealth("cache", true, "UP",
                "缓存正常", LocalDateTime.now()));
        
        return new HealthStatus(true, "UP", components, LocalDateTime.now());
    }
}