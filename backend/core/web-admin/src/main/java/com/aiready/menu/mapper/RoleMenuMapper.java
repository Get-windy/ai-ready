package com.aiready.menu.mapper;

import com.aiready.menu.entity.RoleMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色菜单关联Mapper接口
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {
    
    /**
     * 批量插入角色菜单关联
     */
    int insertBatch(@Param("roleMenus") List<RoleMenu> roleMenus);
    
    /**
     * 根据角色ID删除关联
     */
    int deleteByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 根据菜单ID删除关联
     */
    int deleteByMenuId(@Param("menuId") Long menuId);
    
    /**
     * 根据角色ID获取菜单ID列表
     */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}
