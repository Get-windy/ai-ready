package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 查询所有菜单（树形结构）
     */
    List<SysMenu> selectAllMenus(@Param("tenantId") Long tenantId);

    /**
     * 查询用户的菜单列表
     */
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    /**
     * 查询子菜单
     */
    List<SysMenu> selectChildrenByParentId(@Param("parentId") Long parentId, 
                                            @Param("tenantId") Long tenantId);

    /**
     * 查询角色的菜单ID列表
     */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 检查菜单编码是否存在
     */
    int checkMenuCodeExists(@Param("menuCode") String menuCode,
                            @Param("tenantId") Long tenantId,
                            @Param("excludeId") Long excludeId);

    /**
     * 根据用户ID查询角色ID列表
     */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID列表查询菜单ID列表
     */
    @Select("<script>" +
            "SELECT DISTINCT menu_id FROM sys_role_menu " +
            "WHERE role_id IN " +
            "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>" +
            "#{roleId}" +
            "</foreach>" +
            "</script>")
    List<Long> selectMenuIdsByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 根据菜单ID列表查询菜单
     */
    @Select("<script>" +
            "SELECT * FROM sys_menu WHERE id IN " +
            "<foreach collection='menuIds' item='menuId' open='(' separator=',' close=')'>" +
            "#{menuId}" +
            "</foreach>" +
            " AND deleted = 0 ORDER BY sort" +
            "</script>")
    List<SysMenu> selectMenusByIds(@Param("menuIds") List<Long> menuIds);
}