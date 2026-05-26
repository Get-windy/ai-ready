package cn.aiedge.permission.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限信息VO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PermissionVO {
    
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
     * 菜单组件
     */
    private String component;
    
    /**
     * 菜单图标
     */
    private String icon;
    
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
     * 是否可见
     */
    private Integer visible;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 子权限列表
     */
    private List<PermissionVO> children;
}
