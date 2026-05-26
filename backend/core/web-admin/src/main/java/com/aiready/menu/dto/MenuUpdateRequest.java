package com.aiready.menu.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 菜单更新请求
 */
@Data
public class MenuUpdateRequest {
    
    /**
     * 菜单ID
     */
    @NotNull(message = "菜单ID不能为空")
    private Long id;
    
    /**
     * 父菜单ID（0表示顶级菜单）
     */
    @NotNull(message = "父菜单ID不能为空")
    private Long parentId;
    
    /**
     * 菜单名称
     */
    @NotBlank(message = "菜单名称不能为空")
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
     * 权限标识（多个用逗号分隔）
     */
    private String permissions;
    
    /**
     * 菜单类型（1：目录 2：菜单 3：按钮）
     */
    @NotNull(message = "菜单类型不能为空")
    private Integer menuType;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
    
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
    private Integer keepAlive;
    
    /**
     * 外部链接（0：否 1：是）
     */
    private Integer external;
    
    /**
     * 备注
     */
    private String remark;
}
