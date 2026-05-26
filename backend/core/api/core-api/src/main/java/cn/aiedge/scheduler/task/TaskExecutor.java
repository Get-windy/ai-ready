package cn.aiedge.scheduler.task;

import cn.aiedge.scheduler.mapper.ScheduledTaskLogMapper;
import cn.aiedge.scheduler.mapper.ScheduledTaskMapper;
import cn.aiedge.scheduler.model.ScheduledTask;
import cn.aiedge.scheduler.model.ScheduledTaskLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 任务执行器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskExecutor {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final ScheduledTaskMapper taskMapper;
    private final ScheduledTaskLogMapper logMapper;
    
    // 存储正在运行的任务
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    private String nodeIp;

    @PostConstruct
    public void init() {
        try {
            nodeIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            nodeIp = "unknown";
        }
        
        // 启动时加载所有启用的任务
        loadEnabledTasks();
    }

    /**
     * 加载启用的任务
     */
    public void loadEnabledTasks() {
        taskMapper.selectEnabledTasks().forEach(this::scheduleTask);
    }

    /**
     * 调度任务
     */
    public void scheduleTask(ScheduledTask task) {
        // 先取消已存在的任务
        cancelTask(task.getId());
        
        if (task.getEnabled() != 1) {
            return;
        }
        
        try {
            ScheduledFuture<?> future;
            
            if ("CRON".equals(task.getTaskType())) {
                // Cron表达式调度
                future = taskScheduler.schedule(
                    () -> executeTask(task),
                    new CronTrigger(task.getCronExpression())
                );
            } else if ("FIXED_DELAY".equals(task.getTaskType())) {
                // 固定延迟调度
                future = taskScheduler.scheduleWithFixedDelay(
                    () -> executeTask(task),
                    task.getRetryInterval() * 1000L
                );
            } else {
                // 固定频率调度
                future = taskScheduler.scheduleAtFixedRate(
                    () -> executeTask(task),
                    task.getRetryInterval() * 1000L
                );
            }
            
            scheduledTasks.put(task.getId(), future);
            task.setStatus("RUNNING");
            taskMapper.updateById(task);
            
            log.info("任务调度成功: {}", task.getTaskName());
        } catch (Exception e) {
            log.error("任务调度失败: {}", task.getTaskName(), e);
            task.setStatus("ERROR");
            taskMapper.updateById(task);
        }
    }

    /**
     * 取消任务
     */
    public void cancelTask(Long taskId) {
        ScheduledFuture<?> future = scheduledTasks.get(taskId);
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
            scheduledTasks.remove(taskId);
            log.info("任务已取消: {}", taskId);
        }
    }

    /**
     * 立即执行任务
     */
    public void executeImmediately(ScheduledTask task) {
        taskScheduler.execute(() -> executeTask(task));
    }

    /**
     * 执行任务
     */
    public void executeTask(ScheduledTask task) {
        // 创建执行日志
        ScheduledTaskLog taskLog = new ScheduledTaskLog();
        taskLog.setTaskId(task.getId());
        taskLog.setTaskName(task.getTaskName());
        taskLog.setStartTime(LocalDateTime.now());
        taskLog.setExecuteParams(task.getExecuteParams());
        taskLog.setExecuteNode(nodeIp);
        taskLog.setRetryTimes(0);
        taskLog.setExecuteStatus("RUNNING");
        logMapper.insert(taskLog);
        
        boolean success = false;
        Exception lastException = null;
        
        // 重试机制
        int maxRetries = task.getRetryCount() != null ? task.getRetryCount() : 0;
        for (int i = 0; i <= maxRetries; i++) {
            try {
                executeTaskLogic(task);
                success = true;
                break;
            } catch (Exception e) {
                lastException = e;
                taskLog.setRetryTimes(i + 1);
                
                if (i < maxRetries) {
                    // 等待后重试
                    try {
                        Thread.sleep(task.getRetryInterval() * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // 更新日志
        taskLog.setEndTime(LocalDateTime.now());
        taskLog.setExecuteTime(
            java.time.Duration.between(taskLog.getStartTime(), taskLog.getEndTime()).toMillis()
        );
        
        if (success) {
            taskLog.setExecuteStatus("SUCCESS");
            taskLog.setExecuteResult("执行成功");
            taskMapper.updateExecuteStats(task.getId(), 1, 0);
        } else {
            taskLog.setExecuteStatus("FAILURE");
            taskLog.setErrorMessage(lastException != null ? lastException.getMessage() : "未知错误");
            taskLog.setExceptionStack(getStackTrace(lastException));
            taskMapper.updateExecuteStats(task.getId(), 0, 1);
        }
        
        logMapper.updateById(taskLog);
        
        // 更新下次执行时间
        task.setLastExecuteTime(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    /**
     * 执行任务逻辑
     */
    private void executeTaskLogic(ScheduledTask task) throws Exception {
        // 使用反射执行指定的方法
        Class<?> clazz = Class.forName(task.getExecuteClass());
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Method method = clazz.getMethod(task.getExecuteMethod(), String.class);
        method.invoke(instance, task.getExecuteParams());
    }

    /**
     * 获取异常堆栈
     */
    private String getStackTrace(Exception e) {
        if (e == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }
        return sb.toString();
    }

    /**
     * 定时检查任务状态
     */
    @Scheduled(fixedRate = 60000)
    public void checkTasks() {
        // 检查是否有新启用的任务
        loadEnabledTasks();
    }
}
