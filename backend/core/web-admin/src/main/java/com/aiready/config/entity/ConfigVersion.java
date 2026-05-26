package com.aiready.config.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 配置版本历史实体类
 * 用于记录配置变更历史，支持版本回滚
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_config_version")
public class ConfigVersion {
    
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
     * 版本号
     */
    private Integer version;
    
    /**
     * 配置值
     */
    private String configValue;
    
    /**
     * 变更类型（CREATE：创建 UPDATE：更新 DELETE：删除）
     */
    private String changeType;
    
    /**
     * 变更原因
     */
    private String changeReason;
    
    /**
     * 变更前值
     */
    private String oldValue;
    
    /**
     * 变更后值
     */
    private String newValue;
    
    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
