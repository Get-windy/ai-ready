package cn.aiedge.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 权限数据传输对象
 * 使用 Java 17 Record 简化代码
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public sealed interface PermissionDTO permits PermissionDTO.Create, PermissionDTO.Update, PermissionDTO.Query {

    /**
     * 创建权限DTO
     */
    record Create(
            Long tenantId,
            Long parentId,
            @NotBlank(message = "权限名称不能为空")
            @Size(min = 2, max = 50, message = "权限名称长度2-50字符")
            String permissionName,
            @NotBlank(message = "权限编码不能为空")
            @Size(min = 2, max = 100, message = "权限编码长度2-100字符")
            String permissionCode,
            Integer permissionType,
            String path,
            String component,
            String icon,
            String apiPath,
            String method,
            Integer sort,
            Integer visible
    ) implements PermissionDTO {}

    /**
     * 更新权限DTO
     */
    record Update(
            Long id,
            Long parentId,
            String permissionName,
            String permissionCode,
            Integer permissionType,
            String path,
            String component,
            String icon,
            String apiPath,
            String method,
            Integer sort,
            Integer visible
    ) implements PermissionDTO {}

    /**
     * 查询权限DTO
     */
    record Query(
            Long tenantId,
            Long parentId,
            String permissionName,
            Integer permissionType,
            Integer status,
            Integer pageNum,
            Integer pageSize
    ) implements PermissionDTO {}
}