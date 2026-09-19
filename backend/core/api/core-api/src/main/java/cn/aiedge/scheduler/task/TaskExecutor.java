package cn.aiedge.scheduler.task;

import cn.aiedge.base.scheduler.JobHandler;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.scheduler.job.JobHandlerRegistry;
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
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
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
    /** 执行目标白名单（按 job_key 解析处理器） */
    private final JobHandlerRegistry jobHandlerRegistry;
    
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
     * 立即执行任务（无发起人信息 —— 由 {@link #executeTask(ScheduledTask)} 兜底解析）
     */
    public void executeImmediately(ScheduledTask task) {
        executeImmediately(task, null, null);
    }

    /**
     * 立即执行任务（**带发起人**）。
     *
     * <p>⚠️ 发起人必须由<b>调用线程</b>解析后透传：{@code taskScheduler.execute(...)} 会切到调度线程池，
     * Sa-Token 的会话上下文（ThreadLocal）在新线程里取不到，若在 {@link #executeTask} 里现取，
     * 「立即执行 / 重试」的台账行就会丢发起人。</p>
     *
     * @param operatorId   发起人用户 id（可为 null）
     * @param operatorName 发起人姓名（为空时退回会话解析 / 系统标识）
     */
    public void executeImmediately(ScheduledTask task, Long operatorId, String operatorName) {
        taskScheduler.execute(() -> executeTask(task, operatorId, operatorName));
    }

    /**
     * 执行任务（无发起人信息：调度线程触发 → 记系统标识「定时调度」）
     */
    public void executeTask(ScheduledTask task) {
        executeTask(task, null, null);
    }

    /**
     * 执行任务（并记录发起人）。
     *
     * <p><b>发起人写入口径（2026-09-18 补齐 README §10.5「创建人恒为 -」缺口）</b>：
     * ① 显式透传的发起人（立即执行 / 重试）→ 记其 id + 姓名；
     * ② 当前线程有会话（兜底）→ 记会话用户；
     * ③ 都没有（无会话的定时调度）→ 记系统标识「定时调度」，**绝不留空**。</p>
     */
    public void executeTask(ScheduledTask task, Long operatorId, String operatorName) {
        // 创建执行日志
        ScheduledTaskLog taskLog = new ScheduledTaskLog();
        taskLog.setTaskId(task.getId());
        taskLog.setTaskName(task.getTaskName());
        taskLog.setStartTime(LocalDateTime.now());
        taskLog.setExecuteParams(task.getExecuteParams());
        taskLog.setExecuteNode(nodeIp);
        taskLog.setRetryTimes(0);
        taskLog.setExecuteStatus("RUNNING");
        // 任务类型：本执行器产出的行**只可能**来自 scheduled_task 的定义执行（事实陈述，非造数），
        // 与 V11.401.0 对既有行的回填口径一致（否则新行的「任务类型」列会空白）
        taskLog.setTaskType("定时任务");
        // 发起人：台账「创建人」列的数据来源（写入侧此前从未写过，故历史行恒为「-」）
        Initiator initiator = resolveInitiator(operatorId, operatorName);
        taskLog.setCreateBy(initiator.id());
        taskLog.setCreatedByName(initiator.name());
        logMapper.insert(taskLog);
        
        boolean success = false;
        Exception lastException = null;
        String executeSummary = null;

        // 重试机制
        int maxRetries = task.getRetryCount() != null ? task.getRetryCount() : 0;
        for (int i = 0; i <= maxRetries; i++) {
            try {
                executeSummary = executeTaskLogic(task);
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
            // 处理器自报的执行摘要（否则记「执行成功」）——让「执行日志」能看到真实业务结果
            taskLog.setExecuteResult(org.springframework.util.StringUtils.hasText(executeSummary)
                    ? executeSummary : "执行成功");
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
     * 执行任务逻辑：按 {@code job_key} 取**已注册的处理器**执行
     *
     * <p>2026-09-14 改造：原实现用
     * {@code Class.forName(task.getExecuteClass()).getDeclaredConstructor().newInstance()}
     * + {@code getMethod(method, String.class)} 反射调用 ——
     * ① 只能跑「无参构造 + 单 String 参数」的普通类，**无法注入 Spring Bean**（定时任务形同虚设）；
     * ② 类名/方法名来自请求体 → 任意登录用户可下发（越权面）。现改为白名单处理器解析，
     * 任务行里的类名字段不再被读取。</p>
     */
    private String executeTaskLogic(ScheduledTask task) throws Exception {
        JobHandler handler = jobHandlerRegistry.get(task.getJobKey());
        if (handler == null) {
            throw new IllegalStateException("未注册的任务处理器: job_key=" + task.getJobKey()
                    + "（请改用 GET /api/scheduler/task/handlers 返回的处理器键）");
        }
        return handler.execute(task.getExecuteParams());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  发起人解析（台账「创建人」列）
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 无会话（定时调度线程）触发时的发起人标识。
     * 语义 = 「由系统调度自动执行」，与「某用户手工执行」区分开；<b>绝不留空</b>。
     */
    private static final String SYSTEM_OPERATOR_NAME = "定时调度";

    /** 发起人（id 可空，姓名必非空） */
    private record Initiator(Long id, String name) {
    }

    /**
     * 解析本次执行的发起人：显式透传 > 当前会话 > 系统标识。
     *
     * <p>{@link SecurityUtils} 内部对 Sa-Token 抛出的「非 Web 上下文」异常已做兜底（返回 null），
     * 因此调度线程调用本方法不会抛错。</p>
     */
    private Initiator resolveInitiator(Long operatorId, String operatorName) {
        if (StringUtils.hasText(operatorName)) {
            return new Initiator(operatorId, operatorName);
        }
        Long sessionUserId = SecurityUtils.getCurrentUserId();
        String sessionUsername = SecurityUtils.getCurrentUsername();
        if (sessionUserId != null || StringUtils.hasText(sessionUsername)) {
            return new Initiator(sessionUserId,
                    StringUtils.hasText(sessionUsername) ? sessionUsername : "用户" + sessionUserId);
        }
        // 无会话：定时调度线程触发（平台级）→ 记明确的系统标识，不编造人名
        return new Initiator(null, SYSTEM_OPERATOR_NAME);
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
