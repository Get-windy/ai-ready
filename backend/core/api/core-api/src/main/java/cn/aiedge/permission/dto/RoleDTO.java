package cn.aiedge.permission.dto;

import lombok.Data;

import java.util.List;

/**
 * 角色权限分配DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class RoleDTO {
    
    /**
     * 角色ID
     */
    private Long roleId;
    
    /**
     * 权限ID列表
     */
    private List<Long> permissionIds;
}
