package cn.aiedge.scheduler.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务配置实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@TableName("sys_job")
public class JobConfig {

    /**
     * 任务ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务分组
     */
    private String jobGroup;

    /**
     * 任务描述
     */
    private String description;

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
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 任务状态：0-暂停，1-正常
     */
    private Integer status;

    /**
     * 是否并发：0-禁止，1-允许
     */
    private Integer concurrent;

    /**
     * 错过策略：1-立即执行，2-执行一次，3-放弃执行
     */
    private Integer misfirePolicy;

    /**
     * 下次执行时间
     */
    private LocalDateTime nextTime;

    /**
     * 上次执行时间
     */
    private LocalDateTime prevTime;

    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志
     */
    @TableLogic
    private Integer deleted;
}
