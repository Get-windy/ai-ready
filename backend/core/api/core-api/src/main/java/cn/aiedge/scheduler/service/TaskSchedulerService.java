package cn.aiedge.scheduler.service;

import cn.aiedge.scheduler.entity.ScheduledTask;
import cn.aiedge.scheduler.entity.TaskExecuteLog;

import java.util.List;

/**
 * 定时任务服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface TaskSchedulerService {

    // ==================== 任务管理 ====================

    /**
     * 创建定时任务
     *
     * @param task 任务信息
     * @return 创建的任务
     */
    ScheduledTask createTask(ScheduledTask task);

    /**
     * 更新定时任务
     *
     * @param task 任务信息
     * @return 更新后的任务
     */
    ScheduledTask updateTask(ScheduledTask task);

    /**
     * 删除定时任务
     *
     * @param taskId 任务ID
     * @return 是否删除成功
     */
    boolean deleteTask(Long taskId);

    /**
     * 获取任务详情
     *
     * @param taskId 任务ID
     * @return 任务信息
     */
    ScheduledTask getTask(Long taskId);

    /**
     * 获取所有任务
     *
     * @return 任务列表
     */
    List<ScheduledTask> getAllTasks();

    /**
     * 按分组获取任务
     *
     * @param taskGroup 任务分组
     * @return 任务列表
     */
    List<ScheduledTask> getTasksByGroup(String taskGroup);

    // ==================== 任务调度 ====================

    /**
     * 启动任务
     *
     * @param taskId 任务ID
     * @return 是否启动成功
     */
    boolean startTask(Long taskId);

    /**
     * 暂停任务
     *
     * @param taskId 任务ID
     * @return 是否暂停成功
     */
    boolean pauseTask(Long taskId);

    /**
     * 恢复任务
     *
     * @param taskId 任务ID
     * @return 是否恢复成功
     */
    boolean resumeTask(Long taskId);

    /**
     * 立即执行一次任务
     *
     * @param taskId 任务ID
     * @return 是否触发成功
     */
    boolean triggerTask(Long taskId);

    /**
     * 检查任务是否存在
     *
     * @param taskId 任务ID
     * @return 是否存在
     */
    boolean taskExists(Long taskId);

    // ==================== 任务日志 ====================

    /**
     * 记录任务执行日志
     *
     * @param log 执行日志
     * @return 保存后的日志
     */
    TaskExecuteLog saveLog(TaskExecuteLog log);

    /**
     * 获取任务执行日志
     *
     * @param taskId 任务ID
     * @param limit  限制数量
     * @return 日志列表
     */
    List<TaskExecuteLog> getTaskLogs(Long taskId, int limit);

    /**
     * 清理过期日志
     *
     * @param daysToKeep 保留天数
     * @return 删除数量
     */
    int cleanOldLogs(int daysToKeep);

    // ==================== 调度器控制 ====================

    /**
     * 启动调度器
     */
    void startScheduler();

    /**
     * 停止调度器
     */
    void stopScheduler();

    /**
     * 检查调度器是否运行
     *
     * @return 是否运行中
     */
    boolean isSchedulerRunning();
}
