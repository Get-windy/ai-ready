package com.aiready.config.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配置组DTO
 */
@Data
public class ConfigGroupDTO {
    
    private Long id;
    
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
     * 父组名称
     */
    private String parentName;
    
    /**
     * 层级路径
     */
    private String path;
    
    /**
     * 层级深度
     */
    private Integer depth;
    
    /**
     * 图标
     */
    private String icon;
    
    /**
     * 排序号
     */
    private Integer sortOrder;
    
    /**
     * 是否系统内置
     */
    private Integer builtIn;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 子组列表
     */
    private List<ConfigGroupDTO> children;
    
    /**
     * 配置项数量
     */
    private Integer configCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
