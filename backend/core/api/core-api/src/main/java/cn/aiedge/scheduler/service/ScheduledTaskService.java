package cn.aiedge.scheduler.service;

import cn.aiedge.scheduler.model.ScheduledTask;
import cn.aiedge.scheduler.model.ScheduledTaskLog;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务服务接口
 */
public interface ScheduledTaskService extends IService<ScheduledTask> {

    /**
     * 创建任务
     */
    ScheduledTask createTask(ScheduledTask task);

    /**
     * 更新任务
     */
    ScheduledTask updateTask(ScheduledTask task);

    /**
     * 删除任务
     */
    boolean deleteTask(Long taskId);

    /**
     * 启用任务
     */
    boolean enableTask(Long taskId);

    /**
     * 禁用任务
     */
    boolean disableTask(Long taskId);

    /**
     * 立即执行任务
     */
    boolean executeTask(Long taskId);

    /**
     * 手动重试任务
     */
    boolean retryTask(Long logId);

    /**
     * 暂停任务
     */
    boolean pauseTask(Long taskId);

    /**
     * 恢复任务
     */
    boolean resumeTask(Long taskId);

    /**
     * 获取启用的任务列表
     */
    List<ScheduledTask> getEnabledTasks();

    /**
     * 分页查询任务
     */
    IPage<ScheduledTask> getTaskPage(Page<ScheduledTask> page, String taskName, String status);

    /**
     * 查询任务日志
     */
    IPage<ScheduledTaskLog> getTaskLogPage(Page<ScheduledTaskLog> page, Long taskId,
                                           String status, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取任务统计
     */
    TaskStatistics getTaskStatistics(Long taskId);

    /**
     * 批量执行任务
     */
    void batchExecute(List<Long> taskIds);

    /**
     * 任务统计
     */
    class TaskStatistics {
        private Long totalCount;
        private Long successCount;
        private Long failCount;
        private Double successRate;
        private Long avgExecuteTime;

        // Getters and Setters
        public Long getTotalCount() { return totalCount; }
        public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
        public Long getSuccessCount() { return successCount; }
        public void setSuccessCount(Long successCount) { this.successCount = successCount; }
        public Long getFailCount() { return failCount; }
        public void setFailCount(Long failCount) { this.failCount = failCount; }
        public Double getSuccessRate() { return successRate; }
        public void setSuccessRate(Double successRate) { this.successRate = successRate; }
        public Long getAvgExecuteTime() { return avgExecuteTime; }
        public void setAvgExecuteTime(Long avgExecuteTime) { this.avgExecuteTime = avgExecuteTime; }
    }
}
