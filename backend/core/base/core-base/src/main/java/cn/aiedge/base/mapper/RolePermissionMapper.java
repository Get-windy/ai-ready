package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.RolePermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关联Mapper
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    /**
     * 删除角色的所有权限关联
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除权限的所有角色关联
     */
    int deleteByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * 批量插入角色权限关联
     */
    int batchInsert(@Param("list") List<RolePermission> list);

    /**
     * 查询角色的权限ID列表
     */
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);
}
