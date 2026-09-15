package cn.aiedge.scheduler.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.scheduler.job.JobHandlerRegistry;
import cn.aiedge.scheduler.mapper.ScheduledTaskLogMapper;
import cn.aiedge.scheduler.mapper.ScheduledTaskMapper;
import cn.aiedge.scheduler.model.JobHandlerVO;
import cn.aiedge.scheduler.model.ScheduledTask;
import cn.aiedge.scheduler.model.ScheduledTaskLog;
import cn.aiedge.scheduler.service.ScheduledTaskService;
import cn.aiedge.scheduler.task.TaskExecutor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时任务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskServiceImpl extends ServiceImpl<ScheduledTaskMapper, ScheduledTask>
        implements ScheduledTaskService {

    private final ScheduledTaskMapper taskMapper;
    private final ScheduledTaskLogMapper logMapper;
    private final TaskExecutor taskExecutor;
    private final JobHandlerRegistry jobHandlerRegistry;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduledTask createTask(ScheduledTask task) {
        validateJobKey(task.getJobKey());
        task.setStatus("STOPPED");
        task.setExecuteCount(0);
        task.setSuccessCount(0);
        task.setFailCount(0);
        if (task.getEnabled() == null) {
            task.setEnabled(1);
        }
        taskMapper.insert(task);

        // 如果启用，添加到调度器
        if (task.getEnabled() == 1) {
            taskExecutor.scheduleTask(task);
        }

        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduledTask updateTask(ScheduledTask task) {
        ScheduledTask existing = taskMapper.selectById(task.getId());
        if (existing == null) {
            throw new BusinessException(400, "任务不存在");
        }
        // 未提交 jobKey 时保持原值（updateById 忽略 null 字段），并统一走白名单校验
        if (!StringUtils.hasText(task.getJobKey())) {
            task.setJobKey(existing.getJobKey());
        }
        validateJobKey(task.getJobKey());

        // 如果任务正在运行，先取消
        if ("RUNNING".equals(existing.getStatus())) {
            taskExecutor.cancelTask(existing.getId());
        }

        taskMapper.updateById(task);

        // 以落库后的最终状态决定「重新调度 / 取消调度」
        ScheduledTask latest = taskMapper.selectById(task.getId());
        if (latest.getEnabled() != null && latest.getEnabled() == 1) {
            taskExecutor.scheduleTask(latest);
        } else {
            taskExecutor.cancelTask(latest.getId());
        }
        return latest;
    }

    /**
     * 执行目标白名单校验：{@code job_key} 必须对应一个已注册的 {@code JobHandler}
     *
     * <p>把「类名 + 方法名」换成「处理器键」后，配置阶段即可拦住无效目标 ——
     * 不再像旧实现那样等到执行时才在日志里出现 {@code ClassNotFoundException/NPE}。</p>
     */
    private void validateJobKey(String jobKey) {
        if (!StringUtils.hasText(jobKey)) {
            throw new BusinessException(400, "请选择任务处理器（job_key）");
        }
        if (jobHandlerRegistry.get(jobKey) == null) {
            String available = jobHandlerRegistry.list().stream()
                    .map(JobHandlerVO::getKey).collect(Collectors.joining(", "));
            throw new BusinessException(400, "未注册的任务处理器: " + jobKey
                    + (available.isEmpty() ? "（当前无可用处理器）" : "（可选：" + available + "）"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTask(Long taskId) {
        // 先取消任务
        taskExecutor.cancelTask(taskId);
        return taskMapper.deleteById(taskId) > 0;
    }

    @Override
    public boolean enableTask(Long taskId) {
        ScheduledTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        // 启用前校验目标处理器仍存在（历史演示任务没有 job_key，启用即失败：给出可读原因而非运行时静默失败）
        validateJobKey(task.getJobKey());

        task.setEnabled(1);
        taskMapper.updateById(task);
        taskExecutor.scheduleTask(task);
        return true;
    }

    @Override
    public boolean disableTask(Long taskId) {
        ScheduledTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        
        task.setEnabled(0);
        task.setStatus("STOPPED");
        taskMapper.updateById(task);
        taskExecutor.cancelTask(taskId);
        return true;
    }

    @Override
    public boolean executeTask(Long taskId) {
        ScheduledTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        
        taskExecutor.executeImmediately(task);
        return true;
    }

    @Override
    public boolean retryTask(Long logId) {
        ScheduledTaskLog taskLog = logMapper.selectById(logId);
        if (taskLog == null) {
            return false;
        }
        
        ScheduledTask task = taskMapper.selectById(taskLog.getTaskId());
        if (task == null) {
            return false;
        }
        
        // 使用相同的参数重试
        task.setExecuteParams(taskLog.getExecuteParams());
        taskExecutor.executeImmediately(task);
        return true;
    }

    @Override
    public boolean pauseTask(Long taskId) {
        ScheduledTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        
        task.setStatus("PAUSED");
        taskMapper.updateById(task);
        taskExecutor.cancelTask(taskId);
        return true;
    }

    @Override
    public boolean resumeTask(Long taskId) {
        ScheduledTask task = taskMapper.selectById(taskId);
        if (task == null || task.getEnabled() != 1) {
            return false;
        }
        
        task.setStatus("RUNNING");
        taskMapper.updateById(task);
        taskExecutor.scheduleTask(task);
        return true;
    }

    @Override
    public List<ScheduledTask> getEnabledTasks() {
        return taskMapper.selectEnabledTasks();
    }

    @Override
    public IPage<ScheduledTask> getTaskPage(Page<ScheduledTask> page, String taskName, String status) {
        LambdaQueryWrapper<ScheduledTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(taskName != null, ScheduledTask::getTaskName, taskName)
               .eq(status != null, ScheduledTask::getStatus, status)
               .orderByDesc(ScheduledTask::getCreateTime);
        return taskMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<ScheduledTaskLog> getTaskLogPage(Page<ScheduledTaskLog> page, Long taskId,
                                                   String status, LocalDateTime startTime, LocalDateTime endTime) {
        return logMapper.selectLogPage(page, taskId, status, startTime, endTime);
    }

    @Override
    public TaskStatistics getTaskStatistics(Long taskId) {
        TaskStatistics stats = new TaskStatistics();
        
        Long successCount = logMapper.countByStatus(taskId, "SUCCESS");
        Long failCount = logMapper.countByStatus(taskId, "FAILURE");
        Long totalCount = successCount + failCount;
        
        stats.setTotalCount(totalCount);
        stats.setSuccessCount(successCount);
        stats.setFailCount(failCount);
        stats.setSuccessRate(totalCount > 0 ? (double) successCount / totalCount * 100 : 0);
        
        return stats;
    }

    @Override
    public void batchExecute(List<Long> taskIds) {
        for (Long taskId : taskIds) {
            try {
                executeTask(taskId);
            } catch (Exception e) {
                log.error("批量执行任务失败: taskId={}", taskId, e);
            }
        }
    }
}
