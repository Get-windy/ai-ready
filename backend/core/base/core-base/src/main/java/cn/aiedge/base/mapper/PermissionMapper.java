package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限Mapper
 */
@Mapper
public interface PermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 根据权限编码查询权限
     */
    SysPermission selectByPermissionCode(@Param("permissionCode") String permissionCode);

    /**
     * 根据用户ID查询权限列表
     */
    List<SysPermission> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询权限列表
     */
    List<SysPermission> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询子权限列表
     */
    List<SysPermission> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 查询菜单类型的权限列表（用于前端菜单渲染）
     */
    List<SysPermission> selectMenuPermissions(@Param("userId") Long userId);
}
