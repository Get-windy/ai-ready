package com.aiready.menu.dto;

import lombok.Data;

/**
 * 菜单查询请求
 */
@Data
public class MenuQueryRequest {
    
    /**
     * 菜单名称
     */
    private String menuName;
    
    /**
     * 菜单编码
     */
    private String menuCode;
    
    /**
     * 菜单类型（1：目录 2：菜单 3：按钮）
     */
    private Integer menuType;
    
    /**
     * 状态（0：禁用 1：启用）
     */
    private Integer status;
    
    /**
     * 父菜单ID
     */
    private Long parentId;
    
    /**
     * 是否只显示启用状态
     */
    private Boolean onlyEnabled = false;
    
    /**
     * 是否构建树形结构
     */
    private Boolean buildTree = true;
}
