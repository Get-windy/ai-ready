package cn.aiedge.scheduler.service.impl;

import cn.aiedge.scheduler.mapper.ScheduledTaskLogMapper;
import cn.aiedge.scheduler.mapper.ScheduledTaskMapper;
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

import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduledTask createTask(ScheduledTask task) {
        task.setStatus("STOPPED");
        task.setExecuteCount(0);
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setEnabled(1);
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
            throw new RuntimeException("任务不存在");
        }
        
        // 如果任务正在运行，先取消
        if ("RUNNING".equals(existing.getStatus())) {
            taskExecutor.cancelTask(existing.getId());
        }
        
        taskMapper.updateById(task);
        
        // 重新调度
        if (task.getEnabled() == 1) {
            ScheduledTask updated = taskMapper.selectById(task.getId());
            taskExecutor.scheduleTask(updated);
        }
        
        return taskMapper.selectById(task.getId());
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
