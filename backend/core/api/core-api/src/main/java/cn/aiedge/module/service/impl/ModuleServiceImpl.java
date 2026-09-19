package cn.aiedge.module.service.impl;

import cn.aiedge.audit.mapper.AuditLogMapper;
import cn.aiedge.audit.model.AuditLog;
import cn.aiedge.base.entity.SysTenantModule;
import cn.aiedge.base.mapper.SysTenantModuleMapper;
import cn.aiedge.module.mapper.SysModuleMapper;
import cn.aiedge.module.mapper.SysModuleVersionMapper;
import cn.aiedge.module.model.SysModule;
import cn.aiedge.module.model.SysModuleVersion;
import cn.aiedge.module.service.ModuleService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模块管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleServiceImpl implements ModuleService {

    private final SysModuleMapper sysModuleMapper;
    private final SysModuleVersionMapper sysModuleVersionMapper;
    private final AuditLogMapper auditLogMapper;
    private final SysTenantModuleMapper sysTenantModuleMapper;

    @Override
    public List<SysModule> getModuleList() {
        LambdaQueryWrapper<SysModule> wrapper = new LambdaQueryWrapper<SysModule>()
                .eq(SysModule::getDeleted, 0)
                .orderByAsc(SysModule::getSortOrder);
        return sysModuleMapper.selectList(wrapper);
    }

    @Override
    public IPage<SysModule> pageModules(String keyword, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<SysModule> wrapper = new LambdaQueryWrapper<SysModule>()
                .eq(SysModule::getDeleted, 0)
                // 关键词同时匹配「模块名称」与「模块编码」
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysModule::getModuleName, keyword)
                        .or().like(SysModule::getModuleCode, keyword))
                .eq(status != null, SysModule::getStatus, status)
                .orderByAsc(SysModule::getSortOrder);
        return sysModuleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public SysModule getModuleById(Long id) {
        return sysModuleMapper.selectById(id);
    }

    @Override
    public SysModule createModule(SysModule module) {
        LocalDateTime now = LocalDateTime.now();
        module.setDeleted(0);
        module.setCreateTime(now);
        module.setUpdateTime(now);
        if (module.getStatus() == null) module.setStatus(1);
        if (module.getSortOrder() == null) module.setSortOrder(0);
        sysModuleMapper.insert(module);
        log.info("创建模块: id={}, name={}", module.getId(), module.getModuleName());
        return module;
    }

    @Override
    public SysModule updateModule(SysModule module) {
        SysModule existing = sysModuleMapper.selectById(module.getId());
        if (existing == null || existing.getDeleted() == 1) return null;
        module.setCreateTime(existing.getCreateTime());
        module.setUpdateTime(LocalDateTime.now());
        module.setDeleted(existing.getDeleted());
        sysModuleMapper.updateById(module);
        log.info("更新模块: id={}, name={}", module.getId(), module.getModuleName());
        return module;
    }

    @Override
    public boolean deleteModule(Long id) {
        SysModule m = sysModuleMapper.selectById(id);
        if (m == null || m.getDeleted() == 1) return false;
        m.setDeleted(1);
        m.setUpdateTime(LocalDateTime.now());
        sysModuleMapper.updateById(m);
        log.info("删除模块: id={}", id);
        return true;
    }

    @Override
    public boolean toggleStatus(Long id) {
        SysModule m = sysModuleMapper.selectById(id);
        if (m == null || m.getDeleted() == 1) return false;
        m.setStatus(m.getStatus() == 1 ? 0 : 1);
        m.setUpdateTime(LocalDateTime.now());
        sysModuleMapper.updateById(m);
        log.info("切换模块状态: id={}, newStatus={}", id, m.getStatus());
        return true;
    }

    @Override
    public List<SysModuleVersion> getVersionList() {
        // PostgreSQL 下 ORDER BY release_time DESC 是 NULLS FIRST —— 未发布的草稿（release_time 为 NULL）
        // 会排在最前。显式 NULLS LAST 让草稿沉底（平台侧版本台账按发布时间倒序才有意义）。
        LambdaQueryWrapper<SysModuleVersion> wrapper = new LambdaQueryWrapper<SysModuleVersion>()
                .last("ORDER BY release_time DESC NULLS LAST");
        return sysModuleVersionMapper.selectList(wrapper);
    }

    @Override
    public IPage<SysModuleVersion> pageVersions(Long moduleId, String releaseStatus, String version,
                                                int pageNum, int pageSize) {
        LambdaQueryWrapper<SysModuleVersion> wrapper = new LambdaQueryWrapper<SysModuleVersion>()
                .eq(moduleId != null, SysModuleVersion::getModuleId, moduleId)
                .eq(StringUtils.hasText(releaseStatus), SysModuleVersion::getReleaseStatus, releaseStatus)
                .like(StringUtils.hasText(version), SysModuleVersion::getVersion, version)
                // PostgreSQL 下 ORDER BY ... DESC 默认 NULLS FIRST，未发布的草稿会顶到最前
                .last("ORDER BY release_time DESC NULLS LAST");
        return sysModuleVersionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public boolean rollbackVersion(Long moduleId, String version) {
        SysModule module = sysModuleMapper.selectById(moduleId);
        if (module == null || module.getDeleted() == 1) return false;
        boolean exists = sysModuleVersionMapper.exists(
                new LambdaQueryWrapper<SysModuleVersion>()
                        .eq(SysModuleVersion::getModuleId, moduleId)
                        .eq(SysModuleVersion::getVersion, version));
        if (!exists) {
            log.warn("回滚失败，目标版本不存在: moduleId={}, version={}", moduleId, version);
            return false;
        }
        module.setVersion(version);
        module.setUpdateTime(LocalDateTime.now());
        sysModuleMapper.updateById(module);
        log.info("回滚模块版本: moduleId={}, version={}", moduleId, version);
        return true;
    }

    /** 当前操作人（无会话上下文时回退 system） */
    private String currentOperator() {
        try {
            return StpUtil.isLogin() ? StpUtil.getLoginIdAsString() : "system";
        } catch (Exception e) {
            return "system";
        }
    }

    @Override
    public SysModuleVersion publishVersion(Long moduleId, String version, String changelog) {
        SysModule module = sysModuleMapper.selectById(moduleId);
        if (module == null || module.getDeleted() == 1) {
            log.warn("发布版本失败，模块不存在: moduleId={}", moduleId);
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        SysModuleVersion v = new SysModuleVersion();
        v.setModuleId(moduleId);
        v.setModuleName(module.getModuleName());
        v.setVersion(version);
        v.setChangelog(changelog);
        v.setReleaseStatus("released");
        // 发布人取真实登录用户（此前硬编码 "admin"）；平台级数据 tenant_id 固定 0
        // （与 sys_module / sys_module_version 存量行的 tenant_id=0 口径一致，此前硬编码 1L 与存量冲突）
        v.setPublisher(currentOperator());
        v.setReleaseTime(now);
        v.setTenantId(0L);
        v.setCreateTime(now);
        v.setUpdateTime(now);
        sysModuleVersionMapper.insert(v);

        // 同步更新模块当前版本号
        module.setVersion(version);
        module.setUpdateTime(now);
        sysModuleMapper.updateById(module);

        log.info("发布版本: moduleId={}, version={}", moduleId, version);
        return v;
    }

    @Override
    public Map<String, Object> getUsageStats(int days) {
        int window = days > 0 ? days : 30;
        LocalDateTime windowStart = LocalDateTime.now().minusDays(window);

        // 从 sys_module 获取所有未删除模块
        List<SysModule> modules = sysModuleMapper.selectList(
                new LambdaQueryWrapper<SysModule>()
                        .eq(SysModule::getDeleted, 0)
                        .orderByAsc(SysModule::getSortOrder)
        );

        // 从 sys_audit_log 统计近 N 天每个模块的调用次数和活跃用户数
        List<AuditLog> recentLogs = auditLogMapper.selectList(
                new LambdaQueryWrapper<AuditLog>()
                        .ge(AuditLog::getOperTime, windowStart)
        );

        // 真实「已授权租户数」：按模块编码统计 sys_tenant_module 的去重租户数
        // （此前该字段硬编码为 1 —— 属假数据，见《使用统计开发文档》§3.6）
        List<SysTenantModule> grants = sysTenantModuleMapper.selectList(
                new LambdaQueryWrapper<SysTenantModule>()
                        .eq(SysTenantModule::getDeleted, 0)
        );
        Map<String, Set<Long>> tenantSets = new HashMap<>();
        for (SysTenantModule g : grants) {
            if (StringUtils.hasText(g.getModuleCode()) && g.getTenantId() != null) {
                tenantSets.computeIfAbsent(g.getModuleCode(), k -> new HashSet<>()).add(g.getTenantId());
            }
        }

        // 按 module 分组统计
        Map<String, Long> callCountMap = recentLogs.stream()
                .filter(l -> l.getModule() != null)
                .collect(Collectors.groupingBy(AuditLog::getModule, Collectors.counting()));

        Map<String, Set<Long>> activeUserMap = new HashMap<>();
        for (AuditLog log : recentLogs) {
            if (log.getModule() != null && log.getUserId() != null) {
                activeUserMap.computeIfAbsent(log.getModule(), k -> new HashSet<>()).add(log.getUserId());
            }
        }

        long maxMonthlyActive = activeUserMap.values().stream().mapToInt(Set::size).max().orElse(1);

        List<Map<String, Object>> stats = new ArrayList<>();
        int rank = 1;

        // 按月度活跃用户数排序
        List<SysModule> sortedModules = modules.stream()
                .sorted((a, b) -> {
                    long aActive = activeUserMap.getOrDefault(a.getModuleCode(), Collections.emptySet()).size();
                    long bActive = activeUserMap.getOrDefault(b.getModuleCode(), Collections.emptySet()).size();
                    return Long.compare(bActive, aActive);
                })
                .collect(Collectors.toList());

        for (SysModule module : sortedModules) {
            String code = module.getModuleCode();
            long monthlyActive = activeUserMap.getOrDefault(code, Collections.emptySet()).size();
            long callCount = callCountMap.getOrDefault(code, 0L);

            // usageRate: 按活跃用户数占比计算百分比
            int usageRate = maxMonthlyActive > 0
                    ? (int) Math.round((double) monthlyActive / maxMonthlyActive * 100)
                    : (module.getStatus() == 1 ? 50 : 0);

            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("rank", rank++);
            stat.put("moduleName", module.getModuleName());
            stat.put("moduleCode", code);
            stat.put("status", module.getStatus());
            stat.put("tenantCount", (long) tenantSets.getOrDefault(code, Collections.emptySet()).size());
            stat.put("usageRate", Math.min(usageRate, 100));
            stat.put("monthlyActive", (int) monthlyActive);
            stat.put("callCount", callCount);
            stats.add(stat);
        }

        // 汇总卡（此前 4 张卡在前端写死 12/10/48/78，本系统无任何数据来源）
        long totalModules = modules.size();
        long enabledModules = modules.stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == 1)
                .count();
        long grantedTenants = grants.stream()
                .map(SysTenantModule::getTenantId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        int avgUsageRate = stats.isEmpty() ? 0
                : (int) Math.round(stats.stream()
                        .mapToInt(s -> ((Number) s.get("usageRate")).intValue())
                        .average().orElse(0));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalModules", totalModules);
        summary.put("enabledModules", enabledModules);
        summary.put("grantedTenants", grantedTenants);
        summary.put("avgUsageRate", avgUsageRate);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", stats);
        result.put("total", stats.size());
        result.put("summary", summary);
        result.put("windowDays", window);
        result.put("windowStart", windowStart.toLocalDate().toString());
        return result;
    }
}
