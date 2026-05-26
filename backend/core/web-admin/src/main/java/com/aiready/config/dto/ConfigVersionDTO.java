package com.aiready.config.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配置版本DTO
 */
@Data
public class ConfigVersionDTO {
    
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
     * 配置名称
     */
    private String configName;
    
    /**
     * 版本号
     */
    private Integer version;
    
    /**
     * 配置值
     */
    private String configValue;
    
    /**
     * 变更类型
     */
    private String changeType;
    
    /**
     * 变更类型名称
     */
    private String changeTypeName;
    
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
    private Long createBy;
    
    /**
     * 创建者名称
     */
    private String createByName;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
