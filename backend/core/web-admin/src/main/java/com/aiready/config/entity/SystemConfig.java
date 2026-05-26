package com.aiready.config.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统配置项实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_system_config")
public class SystemConfig {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 配置键（唯一标识）
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
     * 数据类型（1：字符串 2：整数 3：浮点数 4：布尔值 5：JSON）
     */
    private Integer dataType;
    
    /**
     * 默认值
     */
    private String defaultValue;
    
    /**
     * 是否可编辑（0：否 1：是）
     */
    private Integer editable;
    
    /**
     * 是否可删除（0：否 1：是）
     */
    private Integer deletable;
    
    /**
     * 是否敏感配置（0：否 1：是，敏感配置值加密存储）
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
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 删除标志（0：未删除 1：已删除）
     */
    @TableLogic
    private Integer deleted;
}
