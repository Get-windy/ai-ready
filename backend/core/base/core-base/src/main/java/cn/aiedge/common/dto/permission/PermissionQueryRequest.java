package cn.aiedge.common.dto.permission;

import lombok.Data;

/**
 * 权限查询请求DTO
 */
@Data
public class PermissionQueryRequest {

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 权限名称（模糊查询）
     */
    private String permissionName;

    /**
     * 权限类型
     */
    private String permissionType;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 父权限ID
     */
    private Long parentId;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
