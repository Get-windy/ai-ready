package com.aiready.config.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配置查询请求
 */
@Data
public class ConfigQueryRequest {
    
    /**
     * 配置键（支持模糊查询）
     */
    private String configKey;
    
    /**
     * 配置名称（支持模糊查询）
     */
    private String configName;
    
    /**
     * 配置组ID
     */
    private Long groupId;
    
    /**
     * 配置组编码
     */
    private String groupCode;
    
    /**
     * 数据类型
     */
    private Integer dataType;
    
    /**
     * 是否敏感配置
     */
    private Integer sensitive;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 创建开始时间
     */
    private LocalDateTime createTimeStart;
    
    /**
     * 创建结束时间
     */
    private LocalDateTime createTimeEnd;
    
    /**
     * 页码
     */
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 20;
}
