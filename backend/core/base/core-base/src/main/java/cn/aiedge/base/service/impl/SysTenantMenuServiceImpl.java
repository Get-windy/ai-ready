package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysTenantMenu;
import cn.aiedge.base.mapper.SysTenantMenuMapper;
import cn.aiedge.base.service.SysTenantMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户-菜单授权服务实现
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SysTenantMenuServiceImpl extends ServiceImpl<SysTenantMenuMapper, SysTenantMenu>
        implements SysTenantMenuService {

    private final SysTenantMenuMapper tenantMenuMapper;

    @Override
    public Set<Long> getAuthorizedMenuIds(Long tenantId) {
        List<Long> menuIds = tenantMenuMapper.selectMenuIdsByTenantId(tenantId);
        if (menuIds == null || menuIds.isEmpty()) {
            return Collections.emptySet();
        }
        return menuIds.stream().collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long tenantId, List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        // 先清除旧授权
        tenantMenuMapper.deleteByTenantId(tenantId);
        // 批量插入新授权
        List<SysTenantMenu> list = menuIds.stream().map(menuId -> {
            SysTenantMenu tm = new SysTenantMenu();
            tm.setTenantId(tenantId);
            tm.setMenuId(menuId);
            tm.setCreateTime(LocalDateTime.now());
            return tm;
        }).collect(Collectors.toList());
        tenantMenuMapper.batchInsert(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMenus(Long tenantId, List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        // 逐条删除指定菜单授权（批量删除需要写自定义SQL）
        for (Long menuId : menuIds) {
            lambdaUpdate()
                    .eq(SysTenantMenu::getTenantId, tenantId)
                    .eq(SysTenantMenu::getMenuId, menuId)
                    .remove();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearByTenantId(Long tenantId) {
        tenantMenuMapper.deleteByTenantId(tenantId);
    }
}
