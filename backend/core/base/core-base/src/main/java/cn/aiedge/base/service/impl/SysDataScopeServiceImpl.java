package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysDataScope;
import cn.aiedge.base.mapper.SysDataScopeMapper;
import cn.aiedge.base.service.SysDataScopeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数据权限范围服务实现
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDataScopeServiceImpl extends ServiceImpl<SysDataScopeMapper, SysDataScope>
        implements SysDataScopeService {

    private final SysDataScopeMapper sysDataScopeMapper;

    @Override
    public List<SysDataScope> getByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return sysDataScopeMapper.selectByRoleIds(roleIds);
    }

    @Override
    public List<SysDataScope> getByRoleAndTable(List<Long> roleIds, String tableName) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<SysDataScope> results = new java.util.ArrayList<>();
        for (Long roleId : roleIds) {
            List<SysDataScope> scopes = sysDataScopeMapper.selectByRoleAndTable(roleId, tableName);
            if (scopes != null) {
                results.addAll(scopes);
            }
        }
        return results;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleDataScopes(Long roleId, List<SysDataScope> scopes) {
        // 先清空角色的所有数据权限
        LambdaQueryWrapper<SysDataScope> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDataScope::getRoleId, roleId);
        remove(wrapper);

        // 批量新增
        if (scopes != null && !scopes.isEmpty()) {
            scopes.forEach(scope -> {
                scope.setRoleId(roleId);
                scope.setStatus(1);
            });
            saveBatch(scopes);
            log.info("保存角色数据权限规则成功: roleId={}, count={}", roleId, scopes.size());
        }
    }
}
