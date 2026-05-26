package com.aiready.config.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 配置组实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_config_group")
public class ConfigGroup {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 组编码（唯一标识）
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
     * 父组ID（支持层级配置组）
     */
    private Long parentId;
    
    /**
     * 层级路径（如：/base/security）
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
     * 是否系统内置（0：否 1：是，系统内置不可删除）
     */
    private Integer builtIn;
    
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
