package com.aiready.menu.mapper;

import com.aiready.menu.entity.Menu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单Mapper接口
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
    
    /**
     * 获取用户菜单列表
     */
    List<Menu> selectMenusByUserId(@Param("userId") Long userId);
    
    /**
     * 获取角色菜单列表
     */
    List<Menu> selectMenusByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 根据父ID查询子菜单
     */
    List<Menu> selectChildrenByParentId(@Param("parentId") Long parentId);
    
    /**
     * 检查菜单编码是否存在
     */
    Integer checkMenuCodeExists(@Param("menuCode") String menuCode, @Param("excludeId") Long excludeId);
    
    /**
     * 获取最大排序号
     */
    Integer selectMaxSortOrder(@Param("parentId") Long parentId);
    
    /**
     * 批量更新排序
     */
    int batchUpdateSortOrder(@Param("menus") List<Menu> menus);
    
    /**
     * 获取所有启用的菜单
     */
    List<Menu> selectAllActive();
    
    /**
     * 获取用户权限列表
     */
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);
}
