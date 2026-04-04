package cn.aiedge.scheduler.retry;

import cn.aiedge.scheduler.entity.ScheduledTask;
import cn.aiedge.scheduler.entity.TaskExecuteLog;
import cn.aiedge.scheduler.job.ScheduledJob;
import cn.aiedge.scheduler.service.TaskSchedulerService;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 任务重试管理器
 * 处理失败任务的重试逻辑
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskRetryManager {

    private final TaskSchedulerService taskSchedulerService;
    private final StringRedisTemplate redisTemplate;
    
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(2);
    
    /**
     * 重试策略缓存：taskId -> RetryPolicy
     */
    private final Map<Long, RetryPolicy> retryPolicyCache = new ConcurrentHashMap<>();
    
    /**
     * 重试计数器：taskId -> retryCount
     */
    private final Map<Long, Integer> retryCounter = new ConcurrentHashMap<>();
    
    private static final String RETRY_COUNT_KEY = "scheduler:retry:count:";
    private static final String RETRY_LOCK_KEY = "scheduler:retry:lock:";

    /**
     * 执行任务（带重试）
     *
     * @param task   任务信息
     * @param job    任务执行器
     * @param params 执行参数
     * @return 执行结果
     */
    public TaskExecuteLog executeWithRetry(ScheduledTask task, ScheduledJob job, 
                                            Map<String, Object> params) {
        RetryPolicy policy = getRetryPolicy(task);
        
        TaskExecuteLog executeLog = createExecuteLog(task);
        int currentRetry = getRetryCount(task.getId());
        
        try {
            // 记录开始
            executeLog.setStatus(TaskExecuteLog.STATUS_RUNNING);
            executeLog.setStartTime(LocalDateTime.now());
            executeLog = taskSchedulerService.saveLog(executeLog);
            
            // 执行前置钩子
            job.beforeExecute(params);
            
            // 执行任务
            String result = job.execute(params);
            
            // 执行成功
            executeLog.setStatus(TaskExecuteLog.STATUS_SUCCESS);
            executeLog.setResultMessage(result);
            executeLog.setEndTime(LocalDateTime.now());
            executeLog.setDuration(calculateDuration(executeLog));
            
            // 执行后置钩子
            job.afterExecute(params, result, null);
            
            // 清除重试计数
            clearRetryCount(task.getId());
            
            log.info("任务执行成功: taskId={}, taskName={}, duration={}ms", 
                task.getId(), task.getTaskName(), executeLog.getDuration());
            
        } catch (Exception e) {
            log.error("任务执行失败: taskId={}, retryCount={}", task.getId(), currentRetry, e);
            
            // 执行后置钩子
            try {
                job.afterExecute(params, null, e);
            } catch (Exception hookEx) {
                log.warn("执行afterExecute钩子失败", hookEx);
            }
            
            // 检查是否需要重试
            if (shouldRetry(task.getId(), policy, e, currentRetry)) {
                executeLog.setStatus(TaskExecuteLog.STATUS_FAILURE);
                executeLog.setResultMessage("执行失败，等待重试 (" + (currentRetry + 1) + "/" + policy.getMaxRetries() + ")");
                executeLog.setExceptionInfo(e.getMessage());
                executeLog.setEndTime(LocalDateTime.now());
                executeLog.setDuration(calculateDuration(executeLog));
                
                // 记录重试日志
                scheduleRetry(task, job, params, policy, currentRetry);
            } else {
                // 重试次数用完或不可重试
                executeLog.setStatus(TaskExecuteLog.STATUS_FAILURE);
                executeLog.setResultMessage("执行失败，重试次数已用尽");
                executeLog.setExceptionInfo(getFullStackTrace(e));
                executeLog.setEndTime(LocalDateTime.now());
                executeLog.setDuration(calculateDuration(executeLog));
                
                // 清除重试计数
                clearRetryCount(task.getId());
            }
        }
        
        return taskSchedulerService.saveLog(executeLog);
    }

    /**
     * 设置任务重试策略
     */
    public void setRetryPolicy(Long taskId, RetryPolicy policy) {
        retryPolicyCache.put(taskId, policy);
        log.info("设置任务重试策略: taskId={}, maxRetries={}", taskId, policy.getMaxRetries());
    }

    /**
     * 获取任务重试策略
     */
    public RetryPolicy getRetryPolicy(ScheduledTask task) {
        return retryPolicyCache.getOrDefault(task.getId(), RetryPolicy.defaultPolicy());
    }

    /**
     * 获取当前重试次数
     */
    public int getRetryCount(Long taskId) {
        Integer count = retryCounter.get(taskId);
        if (count == null) {
            // 尝试从Redis获取
            String key = RETRY_COUNT_KEY + taskId;
            String value = redisTemplate.opsForValue().get(key);
            count = value != null ? Integer.parseInt(value) : 0;
            retryCounter.put(taskId, count);
        }
        return count;
    }

    /**
     * 清除重试计数
     */
    public void clearRetryCount(Long taskId) {
        retryCounter.remove(taskId);
        redisTemplate.delete(RETRY_COUNT_KEY + taskId);
        log.debug("清除重试计数: taskId={}", taskId);
    }

    /**
     * 手动触发重试
     */
    public boolean manualRetry(Long taskId) {
        ScheduledTask task = taskSchedulerService.getTask(taskId);
        if (task == null) {
            log.warn("任务不存在: {}", taskId);
            return false;
        }
        
        // 重置重试计数
        clearRetryCount(taskId);
        
        // 触发任务
        return taskSchedulerService.triggerTask(taskId);
    }

    // ==================== 私有方法 ====================

    private boolean shouldRetry(Long taskId, RetryPolicy policy, Exception e, int currentRetry) {
        if (!policy.isEnabled()) return false;
        if (currentRetry >= policy.getMaxRetries()) return false;
        return policy.isRetryable(e);
    }

    private void scheduleRetry(ScheduledTask task, ScheduledJob job, 
                               Map<String, Object> params, RetryPolicy policy, int currentRetry) {
        // 增加重试计数
        int nextRetry = currentRetry + 1;
        retryCounter.put(task.getId(), nextRetry);
        
        // 同步到Redis
        String key = RETRY_COUNT_KEY + task.getId();
        redisTemplate.opsForValue().set(key, String.valueOf(nextRetry), Duration.ofHours(24));
        
        // 计算重试间隔
        long intervalMs = policy.calculateInterval(nextRetry);
        
        log.info("安排任务重试: taskId={}, retryCount={}, intervalMs={}", 
            task.getId(), nextRetry, intervalMs);
        
        // 调度重试
        retryExecutor.schedule(() -> {
            try {
                executeWithRetry(task, job, params);
            } catch (Exception e) {
                log.error("重试执行失败: taskId={}", task.getId(), e);
            }
        }, intervalMs, TimeUnit.MILLISECONDS);
    }

    private TaskExecuteLog createExecuteLog(ScheduledTask task) {
        return TaskExecuteLog.builder()
                .taskId(task.getId())
                .taskName(task.getTaskName())
                .taskGroup(task.getTaskGroup())
                .serverIp(getServerIp())
                .threadName(Thread.currentThread().getName())
                .build();
    }

    private Long calculateDuration(TaskExecuteLog log) {
        if (log.getStartTime() == null || log.getEndTime() == null) {
            return 0L;
        }
        return Duration.between(log.getStartTime(), log.getEndTime()).toMillis();
    }

    private String getServerIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getFullStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\t").append(element.toString()).append("\n");
        }
        if (sb.length() > 2000) {
            return sb.substring(0, 2000) + "...";
        }
        return sb.toString();
    }
}
