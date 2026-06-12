package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysMenu;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单Mapper
 * <p>
 * SQL 定义见 resources/mapper/SysMenuMapper.xml
 * 复杂动态 SQL 统一在 XML 中维护，注解仅保留简单查询。
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
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID列表查询菜单ID列表（忽略租户过滤，角色菜单关联为系统级配置）
     */
    @InterceptorIgnore(tenantLine = "true")
    List<Long> selectMenuIdsByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 根据菜单ID列表查询菜单（忽略租户过滤，系统级菜单对所有租户可见）
     */
    @InterceptorIgnore(tenantLine = "true")
    List<SysMenu> selectMenusByIds(@Param("menuIds") List<Long> menuIds);
}