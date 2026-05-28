package com.aiready.menu.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单DTO
 */
@Data
public class MenuDTO {
    
    private Long id;
    
    /**
     * 父菜单ID
     */
    private Long parentId;
    
    /**
     * 父菜单名称
     */
    private String parentName;
    
    /**
     * 菜单名称
     */
    private String menuName;
    
    /**
     * 菜单编码
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
     * 权限标识
     */
    private String permissions;
    
    /**
     * 权限标识列表
     */
    private List<String> permissionList;
    
    /**
     * 菜单类型（1：目录 2：菜单 3：按钮）
     */
    private Integer menuType;
    
    /**
     * 菜单类型描述
     */
    private String menuTypeDesc;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
    
    /**
     * 状态（0：禁用 1：启用）
     */
    private Integer status;
    
    /**
     * 状态描述
     */
    private String statusDesc;
    
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
     * 客户端类型
     */
    private String clientType;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 子菜单列表
     */
    private List<MenuDTO> children;
}
