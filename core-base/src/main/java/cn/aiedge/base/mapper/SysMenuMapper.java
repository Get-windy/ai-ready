package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}