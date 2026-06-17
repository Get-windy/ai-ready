package cn.aiedge.module.service.impl;

import cn.aiedge.audit.mapper.AuditLogMapper;
import cn.aiedge.audit.model.AuditLog;
import cn.aiedge.module.mapper.SysModuleMapper;
import cn.aiedge.module.mapper.SysModuleVersionMapper;
import cn.aiedge.module.model.SysModule;
import cn.aiedge.module.model.SysModuleVersion;
import cn.aiedge.module.service.ModuleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    @Override
    public List<SysModule> getModuleList() {
        LambdaQueryWrapper<SysModule> wrapper = new LambdaQueryWrapper<SysModule>()
                .eq(SysModule::getDeleted, 0)
                .orderByAsc(SysModule::getSortOrder);
        return sysModuleMapper.selectList(wrapper);
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
        LambdaQueryWrapper<SysModuleVersion> wrapper = new LambdaQueryWrapper<SysModuleVersion>()
                .orderByDesc(SysModuleVersion::getReleaseTime);
        return sysModuleVersionMapper.selectList(wrapper);
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
        v.setPublisher("admin");
        v.setReleaseTime(now);
        v.setTenantId(1L);
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
    public List<Map<String, Object>> getUsageStats() {
        // 从 sys_module 获取所有已启用模块
        List<SysModule> modules = sysModuleMapper.selectList(
                new LambdaQueryWrapper<SysModule>()
                        .eq(SysModule::getDeleted, 0)
                        .orderByAsc(SysModule::getSortOrder)
        );

        // 从 sys_audit_log 统计近30天每个模块的调用次数和活跃用户数
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<AuditLog> recentLogs = auditLogMapper.selectList(
                new LambdaQueryWrapper<AuditLog>()
                        .ge(AuditLog::getOperTime, thirtyDaysAgo)
        );

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
            stat.put("tenantCount", 1);
            stat.put("usageRate", Math.min(usageRate, 100));
            stat.put("monthlyActive", (int) monthlyActive);
            stat.put("callCount", callCount);
            stats.add(stat);
        }

        return stats;
    }
}
