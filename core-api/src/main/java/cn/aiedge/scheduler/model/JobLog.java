package cn.aiedge.scheduler.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务执行日志实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@TableName("sys_job_log")
public class JobLog {

    /**
     * 日志ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 任务ID
     */
    private Long jobId;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务分组
     */
    private String jobGroup;

    /**
     * 执行类名
     */
    private String beanName;

    /**
     * 执行方法名
     */
    private String methodName;

    /**
     * 方法参数
     */
    private String params;

    /**
     * 执行状态：0-失败，1-成功
     */
    private Integer status;

    /**
     * 执行结果消息
     */
    private String message;

    /**
     * 异常信息
     */
    private String exception;

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
     * 执行服务器IP
     */
    private String serverIp;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
