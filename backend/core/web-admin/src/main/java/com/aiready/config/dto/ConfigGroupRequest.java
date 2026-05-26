package com.aiready.config.dto;

import lombok.Data;

/**
 * 配置组保存请求
 */
@Data
public class ConfigGroupRequest {
    
    /**
     * 组编码
     */
    private String groupCode;
    
    /**
     * 组名称
     */
    private String groupName;
    
    /**
     * 组描述
     */
    private String description;
    
    /**
     * 父组ID
     */
    private Long parentId;
    
    /**
     * 图标
     */
    private String icon;
    
    /**
     * 排序号
     */
    private Integer sortOrder;
    
    /**
     * 状态
     */
    private Integer status;
}
