package cn.aiedge.permission.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户角色分配DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class UserRoleDTO {
    
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 角色ID列表
     */
    private List<Long> roleIds;
}
