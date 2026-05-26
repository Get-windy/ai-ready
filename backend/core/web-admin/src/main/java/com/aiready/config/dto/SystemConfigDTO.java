package com.aiready.config.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置项DTO
 */
@Data
public class SystemConfigDTO {
    
    private Long id;
    
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
     * 配置组编码
     */
    private String groupCode;
    
    /**
     * 配置组名称
     */
    private String groupName;
    
    /**
     * 数据类型（1：字符串 2：整数 3：浮点数 4：布尔值 5：JSON）
     */
    private Integer dataType;
    
    /**
     * 数据类型名称
     */
    private String dataTypeName;
    
    /**
     * 默认值
     */
    private String defaultValue;
    
    /**
     * 是否可编辑
     */
    private Integer editable;
    
    /**
     * 是否可删除
     */
    private Integer deletable;
    
    /**
     * 是否敏感配置
     */
    private Integer sensitive;
    
    /**
     * 配置排序
     */
    private Integer sortOrder;
    
    /**
     * 状态（0：禁用 1：启用）
     */
    private Integer status;
    
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
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
