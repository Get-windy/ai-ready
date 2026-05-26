package cn.aiedge.common.dto.role;

import lombok.Data;

/**
 * 角色查询请求DTO
 */
@Data
public class RoleQueryRequest {

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色名称（模糊查询）
     */
    private String roleName;

    /**
     * 角色类型
     */
    private String roleType;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
