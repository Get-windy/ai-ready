package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限Mapper
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据权限编码查询权限
     */
    Permission selectByPermissionCode(@Param("permissionCode") String permissionCode);

    /**
     * 根据用户ID查询权限列表
     */
    List<Permission> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询权限列表
     */
    List<Permission> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询子权限列表
     */
    List<Permission> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 查询菜单类型的权限列表（用于前端菜单渲染）
     */
    List<Permission> selectMenuPermissions(@Param("userId") Long userId);
}
