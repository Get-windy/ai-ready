package cn.aiedge.integration.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 同步历史记录
 */
@Data
@Accessors(chain = true)
@TableName("sync_history")
public class SyncHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 关联的同步配置ID */
    private Long configId;

    /** 冗余存储配置名称 */
    private String configName;

    /** 外部系统类型 */
    private String sourceType;

    /** 同步方向 */
    private String syncDirection;

    /** 同步类型: full/incremental */
    private String syncType;

    /** 单据类型 */
    private String billType;

    /** 状态: running/success/failed/partial */
    private String status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 耗时毫秒 */
    private Long durationMs;

    private Integer recordsTotal;

    private Integer recordsSynced;

    private Integer recordsCreated;

    private Integer recordsUpdated;

    private Integer recordsSkipped;

    private Integer recordsFailed;

    /** 错误信息 */
    private String errorMessage;

    /** 详细错误日志 */
    private String errorDetail;

    /** 触发方式: manual/cron */
    private String triggerType;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
