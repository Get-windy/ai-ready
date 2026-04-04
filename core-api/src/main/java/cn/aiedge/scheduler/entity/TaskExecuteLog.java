package cn.aiedge.scheduler.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务执行日志实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_task_execute_log")
public class TaskExecuteLog {

    /**
     * 日志ID
     */
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
     * 任务分组
     */
    private String taskGroup;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行耗时（毫秒）
     */
    private Long duration;

    /**
     * 执行状态：0-成功，1-失败，2-运行中
     */
    private Integer status;

    /**
     * 执行结果消息
     */
    private String resultMessage;

    /**
     * 异常信息
     */
    private String exceptionInfo;

    /**
     * 执行服务器IP
     */
    private String serverIp;

    /**
     * 执行线程名
     */
    private String threadName;

    // ==================== 常量定义 ====================

    /**
     * 执行状态：成功
     */
    public static final int STATUS_SUCCESS = 0;

    /**
     * 执行状态：失败
     */
    public static final int STATUS_FAILURE = 1;

    /**
     * 执行状态：运行中
     */
    public static final int STATUS_RUNNING = 2;
}
