package cn.aiedge.erp.price.engine.execution.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ExecutionResult(
        String executionId,
        String strategyId,
        boolean success,
        ExecutionStatus status,
        List<TargetResult> targetResults,
        BigDecimal totalImpact,
        Map<String, Object> executionContext,
        LocalDateTime startTime,
        LocalDateTime endTime,
        long durationMs,
        String errorMessage,
        String errorCode,
        List<ExecutionLog> logs
) {
    public enum ExecutionStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        PARTIALLY_COMPLETED,
        FAILED,
        CANCELLED,
        TIMEOUT
    }
    
    public record TargetResult(
            String targetId,
            String targetType,
            boolean success,
            BigDecimal originalValue,
            BigDecimal newValue,
            BigDecimal impact,
            List<ActionResult> actionResults,
            String message
    ) {}
    
    public record ActionResult(
            String actionId,
            String actionType,
            boolean success,
            BigDecimal impact,
            Map<String, Object> details,
            String message
    ) {}
    
    public record ExecutionLog(
            LocalDateTime timestamp,
            String level,
            String message,
            Map<String, Object> context
    ) {}
}