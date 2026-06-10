package cn.aiedge.tenant.service;

import cn.aiedge.base.entity.SysTenantModule;
import cn.aiedge.base.mapper.SysTenantModuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户模块调用权服务
 * 管理租户已购买模块的校验、查询等
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantModuleService {

    private final SysTenantModuleMapper tenantModuleMapper;

    /**
     * 获取租户的有效模块编码集合
     * 过滤条件：未删除 + 状态正常 + 未过期
     */
    public Set<String> getValidModuleCodes(Long tenantId) {
        if (tenantId == null) {
            return Collections.emptySet();
        }

        try {
            List<SysTenantModule> modules = tenantModuleMapper.selectList(
                    new LambdaQueryWrapper<SysTenantModule>()
                            .eq(SysTenantModule::getTenantId, tenantId)
                            .eq(SysTenantModule::getStatus, 0)
                            .eq(SysTenantModule::getDeleted, 0)
            );

            LocalDateTime now = LocalDateTime.now();
            return modules.stream()
                    .filter(m -> m.getExpireTime() == null || now.isBefore(m.getExpireTime()))
                    .map(SysTenantModule::getModuleCode)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("获取租户模块调用权失败: tenantId={}", tenantId, e);
            return Collections.emptySet();
        }
    }

    /**
     * 检查租户是否拥有指定模块的有效调用权
     *
     * @param tenantId   租户ID
     * @param moduleCode 模块编码（支持前缀匹配，如 "sale:order" 匹配 "sale" 或 "sale:order"）
     * @return true=有权限
     */
    public boolean hasModuleAccess(Long tenantId, String moduleCode) {
        if (tenantId == null || moduleCode == null) {
            return false;
        }

        Set<String> validCodes = getValidModuleCodes(tenantId);
        if (validCodes.isEmpty()) {
            return false;
        }

        // 精确匹配
        if (validCodes.contains(moduleCode)) {
            return true;
        }

        // 前缀匹配（如 "sale:order:view" 匹配 "sale:order" 或 "sale"）
        String prefix = moduleCode;
        while (prefix.contains(":")) {
            prefix = prefix.substring(0, prefix.lastIndexOf(':'));
            if (validCodes.contains(prefix)) {
                return true;
            }
        }
        return validCodes.contains(prefix);
    }

    /**
     * 获取租户的所有模块记录（包含已过期）
     */
    public List<SysTenantModule> getTenantModules(Long tenantId) {
        if (tenantId == null) {
            return Collections.emptyList();
        }
        return tenantModuleMapper.selectList(
                new LambdaQueryWrapper<SysTenantModule>()
                        .eq(SysTenantModule::getTenantId, tenantId)
                        .eq(SysTenantModule::getDeleted, 0)
        );
    }

    /**
     * 为租户分配模块
     */
    public void assignModule(Long tenantId, String moduleCode, String moduleName,
                             String purchaseType, LocalDateTime expireTime) {
        // 检查是否已存在
        SysTenantModule existing = tenantModuleMapper.selectOne(
                new LambdaQueryWrapper<SysTenantModule>()
                        .eq(SysTenantModule::getTenantId, tenantId)
                        .eq(SysTenantModule::getModuleCode, moduleCode)
        );

        if (existing != null) {
            existing.setPurchaseType(purchaseType);
            existing.setExpireTime(expireTime);
            existing.setStatus(0);
            tenantModuleMapper.updateById(existing);
            log.info("更新租户模块调用权: tenantId={}, module={}, type={}", tenantId, moduleCode, purchaseType);
        } else {
            SysTenantModule module = new SysTenantModule();
            module.setTenantId(tenantId);
            module.setModuleCode(moduleCode);
            module.setModuleName(moduleName);
            module.setPurchaseType(purchaseType);
            module.setExpireTime(expireTime);
            module.setStatus(0);
            tenantModuleMapper.insert(module);
            log.info("分配租户模块调用权: tenantId={}, module={}, type={}", tenantId, moduleCode, purchaseType);
        }
    }

    /**
     * 移除租户模块调用权
     */
    public void removeModule(Long tenantId, String moduleCode) {
        tenantModuleMapper.delete(
                new LambdaQueryWrapper<SysTenantModule>()
                        .eq(SysTenantModule::getTenantId, tenantId)
                        .eq(SysTenantModule::getModuleCode, moduleCode)
        );
        log.info("移除租户模块调用权: tenantId={}, module={}", tenantId, moduleCode);
    }
}
