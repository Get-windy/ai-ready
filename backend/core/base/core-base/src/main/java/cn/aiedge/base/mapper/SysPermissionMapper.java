package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 根据用户ID查询权限列表。
     *
     * <p>⚠️ `sys_permission.status` 的语义是 <b>0 = 正常 / 1 = 禁用</b>
     * （实证：devdb 454 行全为 0；`PermissionInitializationConfig` 的 `setStatus(0); // 启用`）。
     * 2026-09-19 修正：此处原写 `p.status = 1`（方向反了），等于「只取已禁用权限」
     * ⇒ 该查询恒返回空。当前调用链 `RbacService#getUserMenus` → 本方法，
     * 而 `getUserMenus` 目前无调用方，故此前无可见症状；方向修正后若接入即刻可用。
     */
    @Select("SELECT p.* FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.deleted = 0 AND p.status = 0 " +
            "ORDER BY p.sort")
    List<SysPermission> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询权限列表。
     *
     * <p>⚠️ 同 {@link #selectPermissionsByUserId}：`p.status = 0` 才是「正常」，写 1 会恒空。
     */
    @Select("SELECT p.* FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId} AND p.deleted = 0 AND p.status = 0 " +
            "ORDER BY p.sort")
    List<SysPermission> selectPermissionsByRoleId(@Param("roleId") Long roleId);
}