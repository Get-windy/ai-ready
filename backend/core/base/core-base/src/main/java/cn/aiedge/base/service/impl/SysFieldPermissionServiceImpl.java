package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysFieldPermission;
import cn.aiedge.base.mapper.SysFieldPermissionMapper;
import cn.aiedge.base.service.SysFieldPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字段级权限服务实现
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysFieldPermissionServiceImpl extends ServiceImpl<SysFieldPermissionMapper, SysFieldPermission>
        implements SysFieldPermissionService {

    private final SysFieldPermissionMapper sysFieldPermissionMapper;

    @Override
    public List<SysFieldPermission> getByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return sysFieldPermissionMapper.selectByRoleIds(roleIds);
    }

    @Override
    public List<SysFieldPermission> getByRoleAndTable(List<Long> roleIds, String tableName) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return sysFieldPermissionMapper.selectByRoleAndTable(roleIds, tableName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleFieldPermissions(Long roleId, List<SysFieldPermission> permissions) {
        // 先清空角色的所有字段权限
        LambdaQueryWrapper<SysFieldPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFieldPermission::getRoleId, roleId);
        remove(wrapper);

        // 批量新增
        if (permissions != null && !permissions.isEmpty()) {
            permissions.forEach(p -> {
                p.setRoleId(roleId);
                p.setStatus(1);
            });
            saveBatch(permissions);
            log.info("保存角色字段权限成功: roleId={}, count={}", roleId, permissions.size());
        }
    }
}
