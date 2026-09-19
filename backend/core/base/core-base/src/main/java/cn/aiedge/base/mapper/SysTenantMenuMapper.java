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
     *
     * <p>⚠️ `tenantId` 必须作为**独立参数**传入，不能只依赖 `list` 里实体的 `tenantId`：
     * MyBatis-Plus 的 `MetaObjectHandler.insertFill` 会在 INSERT 前把实体的 `tenantId`
     * 覆盖成当前**会话租户**，导致「给租户 A 授权」写到会话租户头上（2026-09-18 实踩）。
     */
    int batchInsert(@Param("tenantId") Long tenantId, @Param("list") List<SysTenantMenu> list);

    /**
     * 根据租户ID删除所有授权
     */
    int deleteByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 根据菜单ID删除所有关联
     */
    int deleteByMenuId(@Param("menuId") Long menuId);
}
