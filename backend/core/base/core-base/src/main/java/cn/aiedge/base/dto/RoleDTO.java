package cn.aiedge.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 角色数据传输对象
 * 使用 Java 17 Record 简化代码
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public sealed interface RoleDTO permits RoleDTO.Create, RoleDTO.Update, RoleDTO.Query {

    /**
     * 创建角色DTO
     */
    record Create(
            Long tenantId,
            @NotBlank(message = "角色名称不能为空")
            @Size(min = 2, max = 50, message = "角色名称长度2-50字符")
            String roleName,
            @NotBlank(message = "角色编码不能为空")
            @Size(min = 2, max = 50, message = "角色编码长度2-50字符")
            String roleCode,
            Integer sort,
            String remark
    ) implements RoleDTO {}

    /**
     * 更新角色DTO
     */
    record Update(
            Long id,
            String roleName,
            String roleCode,
            Integer sort,
            String remark
    ) implements RoleDTO {}

    /**
     * 查询角色DTO
     */
    record Query(
            Long tenantId,
            String roleName,
            Integer status,
            Integer pageNum,
            Integer pageSize
    ) implements RoleDTO {}
}