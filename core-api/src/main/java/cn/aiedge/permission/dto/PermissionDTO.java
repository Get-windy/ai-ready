package cn.aiedge.permission.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PermissionDTO {
    
    /**
     * 权限ID
     */
    private Long id;
    
    /**
     * 父级权限ID
     */
    private Long parentId;
    
    /**
     * 权限名称
     */
    private String permissionName;
    
    /**
     * 权限编码
     */
    private String permissionCode;
    
    /**
     * 权限类型（1-菜单 2-按钮 3-API）
     */
    private Integer permissionType;
    
    /**
     * 菜单路径
     */
    private String path;
    
    /**
     * API路径
     */
    private String apiPath;
    
    /**
     * 请求方法
     */
    private String method;
    
    /**
     * 排序号
     */
    private Integer sort;
    
    /**
     * 状态（0-正常 1-禁用）
     */
    private Integer status;
}
