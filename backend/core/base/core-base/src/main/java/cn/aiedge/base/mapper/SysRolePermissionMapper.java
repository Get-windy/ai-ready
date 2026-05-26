package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysRolePermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色-权限关联Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 批量插入角色权限关联
     */
    int batchInsert(@Param("list") List<SysRolePermission> list);

    /**
     * 根据角色ID删除权限关联
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据权限ID删除角色关联
     */
    int deleteByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * 查询角色的权限ID列表
     */
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);
}
