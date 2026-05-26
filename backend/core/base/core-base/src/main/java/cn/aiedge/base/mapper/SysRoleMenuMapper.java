package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysRoleMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色-菜单关联Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    /**
     * 批量插入角色菜单关联
     */
    int batchInsert(@Param("list") List<SysRoleMenu> list);

    /**
     * 根据角色ID删除菜单关联
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据菜单ID删除角色关联
     */
    int deleteByMenuId(@Param("menuId") Long menuId);

    /**
     * 查询角色的菜单ID列表
     */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}
