package cn.aiedge.scheduler.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 定时任务实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_scheduled_task")
public class ScheduledTask {

    /**
     * 任务ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务分组
     */
    private String taskGroup;

    /**
     * 任务类名（全限定名）
     */
    private String taskClass;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 任务参数（JSON格式）
     */
    private String taskParams;

    /**
     * 任务状态：0-暂停，1-运行
     */
    private Integer status;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 上次执行时间
     */
    private LocalDateTime lastExecuteTime;

    /**
     * 下次执行时间
     */
    private LocalDateTime nextExecuteTime;

    /**
     * 上次执行结果：0-成功，1-失败
     */
    private Integer lastExecuteResult;

    /**
     * 执行次数
     */
    private Long executeCount;

    /**
     * 失败次数
     */
    private Long failCount;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    // ==================== 常量定义 ====================

    /**
     * 任务状态：暂停
     */
    public static final int STATUS_PAUSED = 0;

    /**
     * 任务状态：运行
     */
    public static final int STATUS_RUNNING = 1;

    /**
     * 执行结果：成功
     */
    public static final int RESULT_SUCCESS = 0;

    /**
     * 执行结果：失败
     */
    public static final int RESULT_FAILURE = 1;
}
