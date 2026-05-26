package com.aiready.permission.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_permission")
public class Permission {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 权限编码
     */
    private String permissionCode;
    
    /**
     * 权限名称
     */
    private String permissionName;
    
    /**
     * 父权限ID
     */
    private Long parentId;
    
    /**
     * 权限类型（1：目录 2：菜单 3：按钮）
     */
    private Integer type;
    
    /**
     * 权限路径
     */
    private String path;
    
    /**
     * 组件路径
     */
    private String component;
    
    /**
     * 权限图标
     */
    private String icon;
    
    /**
     * 排序
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
    
    /**
     * 子权限列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<Permission> children;
}
