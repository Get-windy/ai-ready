package cn.aiedge.permission.vo;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 用户权限信息VO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class UserPermissionVO {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 部门ID
     */
    private Long deptId;
    
    /**
     * 角色编码集合
     */
    private Set<String> roles;
    
    /**
     * 权限编码集合
     */
    private Set<String> permissions;
    
    /**
     * 数据权限范围
     */
    private Integer dataScope;
    
    /**
     * 部门及子部门ID
     */
    private Set<Long> deptAndChildIds;
}
