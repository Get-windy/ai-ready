package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysTenantMenu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * 租户-菜单授权服务接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SysTenantMenuService extends IService<SysTenantMenu> {

    /**
     * 获取租户已授权的菜单ID集合
     */
    Set<Long> getAuthorizedMenuIds(Long tenantId);

    /**
     * 批量授权菜单给租户
     */
    void assignMenus(Long tenantId, List<Long> menuIds);

    /**
     * 批量移除租户的菜单授权
     */
    void removeMenus(Long tenantId, List<Long> menuIds);

    /**
     * 清除租户的所有菜单授权
     */
    void clearByTenantId(Long tenantId);
}
