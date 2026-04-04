package cn.aiedge.scheduler.monitor;

import cn.aiedge.scheduler.entity.TaskExecuteLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务执行追踪器
 * 提供实时执行统计和监控
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Component
public class TaskExecutionTracker {

    /**
     * 任务统计：taskId -> TaskStatistics
     */
    private final Map<Long, TaskStatistics> statisticsMap = new ConcurrentHashMap<>();
    
    /**
     * 全局统计
     */
    private final GlobalStatistics globalStats = new GlobalStatistics();
    
    /**
     * 正在执行的任务：taskId -> ExecutionContext
     */
    private final Map<Long, ExecutionContext> runningTasks = new ConcurrentHashMap<>();

    /**
     * 记录任务开始执行
     */
    public void recordStart(Long taskId, String taskName) {
        ExecutionContext context = new ExecutionContext();
        context.setTaskId(taskId);
        context.setTaskName(taskName);
        context.setStartTime(LocalDateTime.now());
        context.setThreadName(Thread.currentThread().getName());
        
        runningTasks.put(taskId, context);
        globalStats.incrementRunning();
        
        logExecution("START", taskId, taskName, null);
    }

    /**
     * 记录任务执行成功
     */
    public void recordSuccess(Long taskId, String taskName, long durationMs) {
        runningTasks.remove(taskId);
        
        TaskStatistics stats = getOrCreateStatistics(taskId);
        stats.incrementSuccess();
        stats.addDuration(durationMs);
        stats.setLastSuccessTime(LocalDateTime.now());
        
        globalStats.incrementSuccess();
        globalStats.decrementRunning();
        
        logExecution("SUCCESS", taskId, taskName, "duration=" + durationMs + "ms");
    }

    /**
     * 记录任务执行失败
     */
    public void recordFailure(Long taskId, String taskName, String errorMessage) {
        runningTasks.remove(taskId);
        
        TaskStatistics stats = getOrCreateStatistics(taskId);
        stats.incrementFailure();
        stats.setLastFailureTime(LocalDateTime.now());
        stats.setLastFailureMessage(errorMessage);
        
        globalStats.incrementFailure();
        globalStats.decrementRunning();
        
        logExecution("FAILURE", taskId, taskName, errorMessage);
    }

    /**
     * 记录任务重试
     */
    public void recordRetry(Long taskId, String taskName, int retryCount) {
        TaskStatistics stats = getOrCreateStatistics(taskId);
        stats.incrementRetry();
        
        globalStats.incrementRetry();
        
        logExecution("RETRY", taskId, taskName, "retryCount=" + retryCount);
    }

    /**
     * 获取任务统计
     */
    public TaskStatistics getStatistics(Long taskId) {
        return statisticsMap.get(taskId);
    }

    /**
     * 获取所有统计
     */
    public Map<Long, TaskStatistics> getAllStatistics() {
        return new ConcurrentHashMap<>(statisticsMap);
    }

    /**
     * 获取全局统计
     */
    public GlobalStatistics getGlobalStatistics() {
        return globalStats;
    }

    /**
     * 获取正在执行的任务
     */
    public Map<Long, ExecutionContext> getRunningTasks() {
        return new ConcurrentHashMap<>(runningTasks);
    }

    /**
     * 重置任务统计
     */
    public void resetStatistics(Long taskId) {
        statisticsMap.remove(taskId);
    }

    /**
     * 重置所有统计
     */
    public void resetAllStatistics() {
        statisticsMap.clear();
        globalStats.reset();
    }

    /**
     * 获取执行摘要
     */
    public ExecutionSummary getSummary() {
        return ExecutionSummary.builder()
                .totalExecutions(globalStats.getTotalExecutions())
                .successCount(globalStats.getSuccessCount())
                .failureCount(globalStats.getFailureCount())
                .retryCount(globalStats.getRetryCount())
                .currentlyRunning(globalStats.getCurrentlyRunning())
                .averageDuration(calculateAverageDuration())
                .build();
    }

    // ==================== 私有方法 ====================

    private TaskStatistics getOrCreateStatistics(Long taskId) {
        return statisticsMap.computeIfAbsent(taskId, k -> new TaskStatistics());
    }

    private long calculateAverageDuration() {
        long totalDuration = 0;
        int count = 0;
        
        for (TaskStatistics stats : statisticsMap.values()) {
            totalDuration += stats.getTotalDuration();
            count += stats.getExecutionCount();
        }
        
        return count > 0 ? totalDuration / count : 0;
    }

    private void logExecution(String event, Long taskId, String taskName, String details) {
        System.out.printf("[TaskTracker] %s | taskId=%d | task=%s | %s%n", 
            event, taskId, taskName, details != null ? details : "");
    }

    // ==================== 内部类 ====================

    /**
     * 任务统计
     */
    @Data
    public static class TaskStatistics {
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger retryCount = new AtomicInteger(0);
        private final AtomicLong totalDuration = new AtomicLong(0);
        private final AtomicLong maxDuration = new AtomicLong(0);
        private final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);
        
        private LocalDateTime lastSuccessTime;
        private LocalDateTime lastFailureTime;
        private String lastFailureMessage;

        public void incrementSuccess() {
            successCount.incrementAndGet();
        }

        public void incrementFailure() {
            failureCount.incrementAndGet();
        }

        public void incrementRetry() {
            retryCount.incrementAndGet();
        }

        public void addDuration(long duration) {
            totalDuration.addAndGet(duration);
            maxDuration.updateAndGet(current -> Math.max(current, duration));
            minDuration.updateAndGet(current -> Math.min(current, duration));
        }

        public int getExecutionCount() {
            return successCount.get() + failureCount.get();
        }

        public long getTotalDuration() {
            return totalDuration.get();
        }

        public long getAverageDuration() {
            int count = getExecutionCount();
            return count > 0 ? totalDuration.get() / count : 0;
        }

        public double getSuccessRate() {
            int total = getExecutionCount();
            return total > 0 ? (double) successCount.get() / total * 100 : 0;
        }
    }

    /**
     * 全局统计
     */
    @Data
    public static class GlobalStatistics {
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong retryCount = new AtomicLong(0);
        private final AtomicInteger currentlyRunning = new AtomicInteger(0);

        public void incrementSuccess() {
            totalExecutions.incrementAndGet();
            successCount.incrementAndGet();
        }

        public void incrementFailure() {
            totalExecutions.incrementAndGet();
            failureCount.incrementAndGet();
        }

        public void incrementRetry() {
            retryCount.incrementAndGet();
        }

        public void incrementRunning() {
            currentlyRunning.incrementAndGet();
        }

        public void decrementRunning() {
            currentlyRunning.decrementAndGet();
        }

        public void reset() {
            totalExecutions.set(0);
            successCount.set(0);
            failureCount.set(0);
            retryCount.set(0);
            currentlyRunning.set(0);
        }

        public double getSuccessRate() {
            long total = totalExecutions.get();
            return total > 0 ? (double) successCount.get() / total * 100 : 0;
        }
    }

    /**
     * 执行上下文
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionContext {
        private Long taskId;
        private String taskName;
        private LocalDateTime startTime;
        private String threadName;
    }

    /**
     * 执行摘要
     */
    @Data
    @Builder
    public static class ExecutionSummary {
        private long totalExecutions;
        private long successCount;
        private long failureCount;
        private long retryCount;
        private int currentlyRunning;
        private long averageDuration;

        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) successCount / totalExecutions * 100 : 0;
        }
    }
}
