package cn.aiedge.dms.task.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调度审计日志（《调度任务开发文档》§3.6 工程约束 3）
 *
 * <p>调度工作台的每次人工干预（指派/改派/取消/异常）与系统动作（自动调度/超时升级）都写一行，
 * 用于「谁在什么时候把任务从谁改派给谁、为什么」的追溯。</p>
 */
@Data
@TableName("dms_task_log")
public class DmsTaskLog {

    /** 动作：指派 */
    public static final String ACTION_ASSIGN = "ASSIGN";
    /** 动作：改派 */
    public static final String ACTION_REASSIGN = "REASSIGN";
    /** 动作：取消指派 */
    public static final String ACTION_UNASSIGN = "UNASSIGN";
    /** 动作：取消任务 */
    public static final String ACTION_CANCEL = "CANCEL";
    /** 动作：标记异常 */
    public static final String ACTION_EXCEPTION = "EXCEPTION";
    /** 动作：自动调度指派 */
    public static final String ACTION_AUTO_ASSIGN = "AUTO_ASSIGN";
    /** 动作：超时升级重派 */
    public static final String ACTION_ESCALATE = "ESCALATE";
    /** 动作：批量指派 */
    public static final String ACTION_BATCH_ASSIGN = "BATCH_ASSIGN";
    /** 动作：批量取消 */
    public static final String ACTION_BATCH_CANCEL = "BATCH_CANCEL";
    /** 动作：渠道回传（外部平台派单结果/状态回写） */
    public static final String ACTION_CHANNEL = "CHANNEL";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long taskId;

    private String taskNo;

    private String action;

    private String actionText;

    private Long fromRiderId;

    private String fromRiderName;

    private Long toRiderId;

    private String toRiderName;

    private String reason;

    private Long operatorId;

    private String operatorName;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
