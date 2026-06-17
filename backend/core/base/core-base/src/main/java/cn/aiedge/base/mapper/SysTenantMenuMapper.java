package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysTenantMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 租户-菜单授权Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysTenantMenuMapper extends BaseMapper<SysTenantMenu> {

    /**
     * 根据租户ID查询已授权的菜单ID列表
     */
    List<Long> selectMenuIdsByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 批量插入租户菜单授权
     */
    int batchInsert(@Param("list") List<SysTenantMenu> list);

    /**
     * 根据租户ID删除所有授权
     */
    int deleteByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 根据菜单ID删除所有关联
     */
    int deleteByMenuId(@Param("menuId") Long menuId);
}
