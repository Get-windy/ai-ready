package cn.aiedge.scheduler.config;

import cn.aiedge.scheduler.entity.TaskExecuteLog;
import cn.aiedge.scheduler.service.impl.TaskSchedulerServiceImpl;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.listeners.JobListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 任务执行监听器
 * 记录任务执行日志
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class TaskExecutionListener extends JobListenerSupport {

    private static final ThreadLocal<Long> startTimeHolder = new ThreadLocal<>();

    @Setter
    @Autowired(required = false)
    private TaskSchedulerServiceImpl.TaskExecuteLogRepository logRepository;

    @Override
    public String getName() {
        return "TaskExecutionListener";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        startTimeHolder.set(System.currentTimeMillis());
        log.info("任务开始执行: {}", context.getJobDetail().getKey().getName());
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        Long startTime = startTimeHolder.get();
        startTimeHolder.remove();

        if (logRepository == null) {
            return;
        }

        try {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();
            Long taskId = dataMap.getLong("taskId");
            String taskName = dataMap.getString("taskName");
            String taskGroup = dataMap.getString("taskGroup");

            long duration = System.currentTimeMillis() - (startTime != null ? startTime : 0L);
            boolean success = jobException == null;

            TaskExecuteLog executeLog = TaskExecuteLog.builder()
                    .taskId(taskId)
                    .taskName(taskName)
                    .taskGroup(taskGroup)
                    .startTime(startTime != null ? 
                            LocalDateTime.now().minusNanos(duration * 1_000_000) : 
                            LocalDateTime.now())
                    .endTime(LocalDateTime.now())
                    .duration(duration)
                    .status(success ? TaskExecuteLog.STATUS_SUCCESS : TaskExecuteLog.STATUS_FAILURE)
                    .resultMessage(success ? "执行成功" : jobException.getMessage())
                    .exceptionInfo(jobException != null ? getStackTrace(jobException) : null)
                    .serverIp(getServerIp())
                    .threadName(Thread.currentThread().getName())
                    .build();

            logRepository.save(executeLog);

            log.info("任务执行完成: {}, 耗时: {}ms, 状态: {}", 
                    context.getJobDetail().getKey().getName(), duration, 
                    success ? "成功" : "失败");

        } catch (Exception e) {
            log.error("保存任务执行日志失败", e);
        }
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        log.warn("任务执行被否决: {}", context.getJobDetail().getKey().getName());
    }

    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    private String getServerIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
