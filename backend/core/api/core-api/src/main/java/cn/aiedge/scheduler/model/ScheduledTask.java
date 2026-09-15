package cn.aiedge.scheduler.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 定时任务实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("scheduled_task")
public class ScheduledTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务描述
     */
    private String taskDesc;

    /**
     * 任务类型: CRON/FIXED_DELAY/FIXED_RATE
     */
    private String taskType;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 执行目标处理器键（{@code JobHandler#key()}，白名单内取值）
     *
     * <p>2026-09-14 起**取代**原「执行类全路径 + 执行方法名」：执行目标由
     * {@code JobHandlerRegistry} 白名单解析，任务行不再携带类名/方法名
     * （原实现按请求体里的类名反射调用任意方法，属越权面）。</p>
     */
    private String jobKey;

    /**
     * 执行参数(JSON格式)：原样传给 {@code JobHandler#execute(String)}
     */
    private String executeParams;

    /**
     * 任务状态: RUNNING/PAUSED/STOPPED
     */
    private String status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 重试间隔(秒)
     */
    private Integer retryInterval;

    /**
     * 超时时间(秒)
     */
    private Integer timeout;

    /**
     * 是否启用: 0-禁用 1-启用
     */
    private Integer enabled;

    /**
     * 最后执行时间
     */
    private LocalDateTime lastExecuteTime;

    /**
     * 下次执行时间
     */
    private LocalDateTime nextExecuteTime;

    /**
     * 执行次数
     */
    private Integer executeCount;

    /**
     * 成功次数
     */
    private Integer successCount;

    /**
     * 失败次数
     */
    private Integer failCount;

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
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
