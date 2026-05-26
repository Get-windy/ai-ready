package com.aiready.config.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 配置审计日志实体类
 * 记录所有配置变更操作
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_config_audit_log")
public class ConfigAuditLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 配置项ID
     */
    private Long configId;
    
    /**
     * 配置键
     */
    private String configKey;
    
    /**
     * 操作类型（CREATE：创建 UPDATE：更新 DELETE：删除 QUERY：查询 EXPORT：导出 IMPORT：导入）
     */
    private String operationType;
    
    /**
     * 操作描述
     */
    private String operationDesc;
    
    /**
     * 操作前数据（JSON格式）
     */
    private String beforeData;
    
    /**
     * 操作后数据（JSON格式）
     */
    private String afterData;
    
    /**
     * 操作人ID
     */
    private Long operatorId;
    
    /**
     * 操作人名称
     */
    private String operatorName;
    
    /**
     * 操作IP
     */
    private String operatorIp;
    
    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
    
    /**
     * 是否成功（0：失败 1：成功）
     */
    private Integer success;
    
    /**
     * 错误信息
     */
    private String errorMsg;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
