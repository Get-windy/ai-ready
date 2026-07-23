package cn.aiedge.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 审批回调补偿日志实体（工作流三期：回调失败补偿）
 * 对应表: workflow_callback_log（V11.26.0）
 *
 * 生命周期：分发前落 pending → 分发结束落 success / failed（含 error 与
 * next_retry_time 指数退避）→ 补偿任务重试，达到 max_retry 记 final-failed，
 * 经人工重试端点可重置重试周期并立即分发。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("workflow_callback_log")
public class WorkflowCallbackLogEntity {

    /**
     * 日志ID（雪花算法，同 workflow_instance）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID（重试线程按此设置临时租户上下文后再分发）
     */
    private Long tenantId;

    /**
     * 流程实例ID（workflow_instance.id）
     */
    private Long instanceId;

    /**
     * 业务单据类型（如 purchase_order / sale_order）
     */
    private String bizType;

    /**
     * 业务单据ID
     */
    private Long bizId;

    /**
     * 终态：approved / rejected / terminated
     */
    private String result;

    /**
     * 终审操作人ID
     */
    private Long operatorId;

    /**
     * 终审操作人姓名
     */
    private String operatorName;

    /**
     * 审批意见（拒绝原因等）
     */
    private String comment;

    /**
     * 补偿状态：pending / success / failed / final-failed
     */
    private String status;

    /**
     * 最近一次失败的错误信息（多个回调失败时以 "; " 连接）
     */
    private String error;

    /**
     * 已重试次数（首次分发失败不计）
     */
    private Integer retryCount;

    /**
     * 最大重试次数，达到后记 final-failed
     */
    private Integer maxRetry;

    /**
     * 下次重试时间（指数退避 1m/5m/15m/30m/1h）
     */
    private LocalDateTime nextRetryTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
