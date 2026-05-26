package com.aiready.permission.mapper;

import com.aiready.permission.entity.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限Mapper接口
 */
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据用户ID查询权限列表
     */
    @Select("SELECT p.* FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.status = 1 AND p.deleted = 0 " +
            "ORDER BY p.sort_order")
    List<Permission> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询权限列表
     */
    @Select("SELECT p.* FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId} AND p.status = 1 AND p.deleted = 0 " +
            "ORDER BY p.sort_order")
    List<Permission> selectPermissionsByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色权限
     */
    void insertBatch(@Param("list") List<com.aiready.permission.entity.RolePermission> rolePermissions);
}
