package com.aiready.menu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 菜单实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_menu")
public class Menu {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 父菜单ID（0表示顶级菜单）
     */
    private Long parentId;
    
    /**
     * 菜单名称
     */
    private String menuName;
    
    /**
     * 菜单编码（唯一标识）
     */
    private String menuCode;
    
    /**
     * 菜单图标
     */
    private String icon;
    
    /**
     * 菜单路径
     */
    private String path;
    
    /**
     * 组件路径
     */
    private String component;
    
    /**
     * 路由名称
     */
    private String routeName;
    
    /**
     * 权限标识（多个用逗号分隔）
     */
    private String permissions;
    
    /**
     * 菜单类型（0：目录 1：菜单 2：按钮）
     */
    private Integer menuType;
    
    /**
     * 排序顺序
     */
    private Integer sort;
    
    /**
     * 状态（0：禁用 1：启用）
     */
    private Integer status;
    
    /**
     * 是否显示（0：隐藏 1：显示）
     */
    private Integer visible;
    
    /**
     * 是否缓存（0：否 1：是）
     */
    private Integer isCache;
    
    /**
     * 外部链接（0：否 1：是）
     */
    private Integer isExternal;
    
    /**
     * 客户端类型
     */
    private String clientType;
    
    /**
     * 备注
     */
    private String remark;
    
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
