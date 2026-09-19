package cn.aiedge.scheduler.service.impl;

import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.base.utils.SecurityUtils;
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
    private final SysUserMapper sysUserMapper;

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

        // 发起人必须在**本线程**（仍有 Sa-Token 会话）解析后透传：
        // executeImmediately 内部会切到调度线程池执行，届时会话上下文已丢失（2026-09-18）
        Operator operator = currentOperator();
        taskExecutor.executeImmediately(task, operator.id(), operator.name());
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
        // 重试同样是「某个用户手工发起」→ 与立即执行同一口径记录发起人
        Operator operator = currentOperator();
        taskExecutor.executeImmediately(task, operator.id(), operator.name());
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

    // ══════════════════════════════════════════════════════════════════════
    //  发起人解析（写入 scheduled_task_log.create_by / created_by_name）
    // ══════════════════════════════════════════════════════════════════════

    /** 发起人（姓名可空 → 由 {@link TaskExecutor} 兜底记系统标识） */
    private record Operator(Long id, String name) {
    }

    /**
     * 解析当前登录用户作为发起人。
     *
     * <p>姓名取 {@code sys_user} 的 {@code real_name → nickname → username} 逐级回退
     * （实测库内 real_name 普遍为空、nickname 有值，直接取 real_name 会又变成空）。</p>
     *
     * <p>取不到会话时返回 {@code (null, null)} —— 由 {@link TaskExecutor} 兜底记
     * 「定时调度」，不在此处编造姓名。</p>
     */
    private Operator currentOperator() {
        Long userId = SecurityUtils.getCurrentUserId();
        String username = SecurityUtils.getCurrentUsername();
        if (userId == null) {
            return new Operator(null, username);
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return new Operator(userId, username);
        }
        String name = StringUtils.hasText(user.getRealName()) ? user.getRealName()
                : (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        return new Operator(userId, StringUtils.hasText(name) ? name : username);
    }
}
