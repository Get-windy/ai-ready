package cn.aiedge.scheduler.service.impl;

import cn.aiedge.scheduler.entity.ScheduledTask;
import cn.aiedge.scheduler.entity.TaskExecuteLog;
import cn.aiedge.scheduler.job.QuartzJobWrapper;
import cn.aiedge.scheduler.service.TaskSchedulerService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 定时任务服务实现（基于Quartz）
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSchedulerServiceImpl implements TaskSchedulerService {

    private final Scheduler scheduler;
    private final ScheduledTaskRepository taskRepository;
    private final TaskExecuteLogRepository logRepository;

    // ==================== 任务管理 ====================

    @Override
    @Transactional
    public ScheduledTask createTask(ScheduledTask task) {
        // 设置默认值
        if (task.getStatus() == null) {
            task.setStatus(ScheduledTask.STATUS_PAUSED);
        }
        if (task.getExecuteCount() == null) {
            task.setExecuteCount(0L);
        }
        if (task.getFailCount() == null) {
            task.setFailCount(0L);
        }

        // 保存到数据库
        task = taskRepository.save(task);

        // 创建Quartz任务
        if (task.getStatus() == ScheduledTask.STATUS_RUNNING) {
            scheduleJob(task);
        }

        log.info("创建定时任务成功: id={}, name={}", task.getId(), task.getTaskName());
        return task;
    }

    @Override
    @Transactional
    public ScheduledTask updateTask(ScheduledTask task) {
        ScheduledTask existing = taskRepository.findById(task.getId())
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + task.getId()));

        // 检查任务是否在运行
        boolean wasRunning = existing.getStatus() == ScheduledTask.STATUS_RUNNING;

        // 如果任务在运行，先删除旧的调度
        if (wasRunning) {
            unscheduleJob(existing);
        }

        // 更新数据库
        existing.setTaskName(task.getTaskName());
        existing.setTaskGroup(task.getTaskGroup());
        existing.setTaskClass(task.getTaskClass());
        existing.setCronExpression(task.getCronExpression());
        existing.setTaskParams(task.getTaskParams());
        existing.setDescription(task.getDescription());

        if (task.getStatus() != null) {
            existing.setStatus(task.getStatus());
        }

        taskRepository.save(existing);

        // 如果状态为运行，重新调度
        if (existing.getStatus() == ScheduledTask.STATUS_RUNNING) {
            scheduleJob(existing);
        }

        log.info("更新定时任务成功: id={}", existing.getId());
        return existing;
    }

    @Override
    @Transactional
    public boolean deleteTask(Long taskId) {
        ScheduledTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        // 删除调度
        unscheduleJob(task);

        // 删除数据库记录（逻辑删除）
        taskRepository.deleteById(taskId);

        log.info("删除定时任务成功: id={}", taskId);
        return true;
    }

    @Override
    public ScheduledTask getTask(Long taskId) {
        return taskRepository.findById(taskId).orElse(null);
    }

    @Override
    public List<ScheduledTask> getAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    public List<ScheduledTask> getTasksByGroup(String taskGroup) {
        return taskRepository.findByTaskGroup(taskGroup);
    }

    // ==================== 任务调度 ====================

    @Override
    @Transactional
    public boolean startTask(Long taskId) {
        ScheduledTask task = getTask(taskId);
        if (task == null) {
            return false;
        }

        try {
            scheduleJob(task);
            task.setStatus(ScheduledTask.STATUS_RUNNING);
            taskRepository.save(task);
            log.info("启动定时任务成功: id={}", taskId);
            return true;
        } catch (Exception e) {
            log.error("启动定时任务失败: id={}", taskId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean pauseTask(Long taskId) {
        ScheduledTask task = getTask(taskId);
        if (task == null) {
            return false;
        }

        try {
            scheduler.pauseJob(jobKey(task));
            task.setStatus(ScheduledTask.STATUS_PAUSED);
            taskRepository.save(task);
            log.info("暂停定时任务成功: id={}", taskId);
            return true;
        } catch (SchedulerException e) {
            log.error("暂停定时任务失败: id={}", taskId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean resumeTask(Long taskId) {
        ScheduledTask task = getTask(taskId);
        if (task == null) {
            return false;
        }

        try {
            scheduler.resumeJob(jobKey(task));
            task.setStatus(ScheduledTask.STATUS_RUNNING);
            taskRepository.save(task);
            log.info("恢复定时任务成功: id={}", taskId);
            return true;
        } catch (SchedulerException e) {
            log.error("恢复定时任务失败: id={}", taskId, e);
            return false;
        }
    }

    @Override
    public boolean triggerTask(Long taskId) {
        ScheduledTask task = getTask(taskId);
        if (task == null) {
            return false;
        }

        try {
            scheduler.triggerJob(jobKey(task));
            log.info("触发定时任务执行: id={}", taskId);
            return true;
        } catch (SchedulerException e) {
            log.error("触发定时任务失败: id={}", taskId, e);
            return false;
        }
    }

    @Override
    public boolean taskExists(Long taskId) {
        return taskRepository.existsById(taskId);
    }

    // ==================== 任务日志 ====================

    @Override
    public TaskExecuteLog saveLog(TaskExecuteLog log) {
        return logRepository.save(log);
    }

    @Override
    public List<TaskExecuteLog> getTaskLogs(Long taskId, int limit) {
        return logRepository.findByTaskIdOrderByStartTimeDesc(taskId, limit);
    }

    @Override
    @Transactional
    public int cleanOldLogs(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        int deleted = logRepository.deleteByStartTimeBefore(cutoffDate);
        log.info("清理过期任务日志: 保留{}天, 删除{}条", daysToKeep, deleted);
        return deleted;
    }

    // ==================== 调度器控制 ====================

    @Override
    public void startScheduler() {
        try {
            if (!scheduler.isStarted()) {
                scheduler.start();
                log.info("调度器已启动");
            }
        } catch (SchedulerException e) {
            log.error("启动调度器失败", e);
            throw new RuntimeException("启动调度器失败", e);
        }
    }

    @Override
    public void stopScheduler() {
        try {
            if (!scheduler.isShutdown()) {
                scheduler.shutdown(true);
                log.info("调度器已停止");
            }
        } catch (SchedulerException e) {
            log.error("停止调度器失败", e);
            throw new RuntimeException("停止调度器失败", e);
        }
    }

    @Override
    public boolean isSchedulerRunning() {
        try {
            return !scheduler.isShutdown() && scheduler.isStarted();
        } catch (SchedulerException e) {
            return false;
        }
    }

    // ==================== 私有方法 ====================

    private void scheduleJob(ScheduledTask task) {
        try {
            JobKey jobKey = jobKey(task);
            JobDetail jobDetail = JobBuilder.newJob(QuartzJobWrapper.class)
                    .withIdentity(jobKey)
                    .usingJobData(QuartzJobWrapper.TASK_ID_KEY, task.getId())
                    .usingJobData(QuartzJobWrapper.TASK_NAME_KEY, task.getTaskName())
                    .usingJobData(QuartzJobWrapper.TASK_CLASS_KEY, task.getTaskClass())
                    .usingJobData(QuartzJobWrapper.TASK_PARAMS_KEY, 
                            StrUtil.blankToDefault(task.getTaskParams(), "{}"))
                    .withDescription(task.getDescription())
                    .storeDurably()
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey(task))
                    .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression())
                            .withMisfireHandlingInstructionDoNothing())
                    .forJob(jobKey)
                    .build();

            // 检查任务是否已存在
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }

            scheduler.scheduleJob(jobDetail, trigger);

            // 更新下次执行时间
            Date nextFireTime = trigger.getNextFireTime();
            if (nextFireTime != null) {
                task.setNextExecuteTime(LocalDateTime.ofInstant(
                        nextFireTime.toInstant(), ZoneId.systemDefault()));
                taskRepository.save(task);
            }

        } catch (SchedulerException e) {
            log.error("调度任务失败: id={}", task.getId(), e);
            throw new RuntimeException("调度任务失败: " + e.getMessage(), e);
        }
    }

    private void unscheduleJob(ScheduledTask task) {
        try {
            JobKey key = jobKey(task);
            if (scheduler.checkExists(key)) {
                scheduler.deleteJob(key);
            }
        } catch (SchedulerException e) {
            log.error("取消调度任务失败: id={}", task.getId(), e);
        }
    }

    private JobKey jobKey(ScheduledTask task) {
        return new JobKey("JOB_" + task.getId(), task.getTaskGroup());
    }

    private TriggerKey triggerKey(ScheduledTask task) {
        return new TriggerKey("TRIGGER_" + task.getId(), task.getTaskGroup());
    }

    // 简化的Repository接口定义（实际应独立文件）
    public interface ScheduledTaskRepository extends org.springframework.data.jpa.repository.JpaRepository<ScheduledTask, Long> {
        List<ScheduledTask> findByTaskGroup(String taskGroup);
    }

    public interface TaskExecuteLogRepository extends org.springframework.data.jpa.repository.JpaRepository<TaskExecuteLog, Long> {
        List<TaskExecuteLog> findByTaskIdOrderByStartTimeDesc(Long taskId, int limit);
        int deleteByStartTimeBefore(LocalDateTime startTime);
    }
}
