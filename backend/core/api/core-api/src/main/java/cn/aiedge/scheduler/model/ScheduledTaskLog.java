package cn.aiedge.scheduler.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 定时任务执行日志
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("scheduled_task_log")
public class ScheduledTaskLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 执行状态: SUCCESS/FAILURE/RUNNING
     */
    private String executeStatus;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行时长(毫秒)
     */
    private Long executeTime;

    /**
     * 执行结果
     */
    private String executeResult;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 异常堆栈
     */
    private String exceptionStack;

    /**
     * 执行参数
     */
    private String executeParams;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 执行节点IP
     */
    private String executeNode;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
