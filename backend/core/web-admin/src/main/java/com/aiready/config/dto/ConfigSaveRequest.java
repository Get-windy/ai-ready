package com.aiready.config.dto;

import lombok.Data;

/**
 * 配置保存请求
 */
@Data
public class ConfigSaveRequest {
    
    /**
     * 配置键
     */
    private String configKey;
    
    /**
     * 配置值
     */
    private String configValue;
    
    /**
     * 配置名称
     */
    private String configName;
    
    /**
     * 配置描述
     */
    private String description;
    
    /**
     * 配置组ID
     */
    private Long groupId;
    
    /**
     * 数据类型（1：字符串 2：整数 3：浮点数 4：布尔值 5：JSON）
     */
    private Integer dataType;
    
    /**
     * 默认值
     */
    private String defaultValue;
    
    /**
     * 是否敏感配置
     */
    private Integer sensitive;
    
    /**
     * 配置排序
     */
    private Integer sortOrder;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 变更原因
     */
    private String changeReason;
}
