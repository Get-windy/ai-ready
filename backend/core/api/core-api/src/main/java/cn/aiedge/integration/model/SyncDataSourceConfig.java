package cn.aiedge.integration.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 同步数据源配置
 *
 * 租户管理员在此配置要绑定的外部系统（来肯云商等）。
 * 每个租户对每种外部系统只能有一条配置。
 */
@Data
@Accessors(chain = true)
@TableName("sync_data_source")
public class SyncDataSourceConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID（多租户隔离） */
    private Long tenantId;

    /** 外部系统类型: ql361=来肯云商 */
    private String sourceType;

    /** 自定义显示名称 */
    private String displayName;

    /** 登录账号 */
    private String sourceUsername;

    /** 登录密码（加密存储） */
    private String sourcePassword;

    /** API 基础地址 */
    private String baseUrl;

    /** 同步方式: full=全量, incremental=增量 */
    private String syncMode;

    /** 同步频率 cron 表达式 */
    private String syncCron;

    /** 心跳检测间隔（秒） */
    private Integer heartbeatInterval;

    /** 同步单据类型列表 (JSON) */
    private String billTypes;

    /** 状态: 0=禁用, 1=启用 */
    private Integer status;

    /** 最后同步时间 */
    private LocalDateTime lastSyncTime;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /** 更新人 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /** 逻辑删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
