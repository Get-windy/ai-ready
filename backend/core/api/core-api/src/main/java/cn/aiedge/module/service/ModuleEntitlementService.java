package cn.aiedge.module.service;

import cn.aiedge.base.entity.SysModulePermission;
import cn.aiedge.base.mapper.SysModulePermissionMapper;
import cn.aiedge.module.model.SysModule;
import cn.aiedge.module.mapper.SysModuleMapper;
import cn.aiedge.tenant.service.TenantModuleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模块 entitlement 判定（「这个租户有没有开通这个模块」）。
 *
 * <p><b>为什么需要它</b>：本仓授权有两层 —— ① 模块授权（平台方决定某租户有没有这个模块）；
 * ② 权限（租户内管理员决定某角色能不能做某件事）。2026-09-21 之前，
 * `TenantModuleService.hasModuleAccess()` **只有读接口调用**，没有任何一处参与请求拦截，
 * 于是平台侧的「模块开关」纯属展示：把仓储模块关掉，仓储接口照样能用。
 * 本类把两层接起来：**接口需要某个权限码 ⇒ 该码归属某个模块 ⇒ 该模块必须已给本租户开通**。</p>
 *
 * <p><b>归属判定口径</b>：权限码 → 模块，查 `sys_module_permission` 的「模块 → 权限码前缀」，
 * <b>最长前缀优先、同长取 sort 小</b>（与 `tools/verify-module-mapping.cjs` 的断言同口径，
 * 该脚本另断言「无同长前缀歧义」）。</p>
 *
 * <p><b>三道有意为之的「不拦」</b>（都是为了让这道门只表达"卖没卖"，不误伤）：
 * <ol>
 *   <li><b>码无归属前缀 → 不拦</b>。典型是 `analytics` 整域还没有权限码族（已知缺口，见 E-08）；
 *       把"查不到归属"当成"未开通"会让任何新写的码当场 403。</li>
 *   <li><b>平台超管 → 不拦</b>（在其调用方 {@code ModuleEntitlementInterceptor} 里判定）。</li>
 *   <li><b>读库失败 → 不拦</b>，但会大声记 ERROR。理由：模块门不是数据隔离的最后一道防线
 *       （数据边界由租户拦截器 + 权限码负责），而一次 DB 抖动若判成"未开通"，
 *       会把**整个租户**打成全站 403 —— 用整站不可用换一条推销口径不划算。
 *       注意这与 {@code TenantModuleService.getValidModuleCodes} 的"吞异常返回空集"不同：
 *       这里刻意选的是「不确定就放行 + 告警」，而不是「不确定就当成没开通」。</li>
 * </ol>
 * </p>
 *
 * @author AI-Ready Team
 * @since 0.3.22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleEntitlementService {

    private final SysModulePermissionMapper modulePermissionMapper;
    private final SysModuleMapper sysModuleMapper;
    private final TenantModuleService tenantModuleService;

    /** 前缀规则有效期：映射表只由迁移改动，5 分钟足够新；出问题可调 evict 立即失效。 */
    private static final long RULES_TTL_MS = Duration.ofMinutes(5).toMillis();
    /** 租户开通集合有效期：平台侧开通/停用后，最迟这么久生效。 */
    private static final long TENANT_TTL_MS = Duration.ofSeconds(30).toMillis();

    /** 被挡住的模块（用于拼 403 文案） */
    public record BlockedModule(String moduleCode, String moduleName) {
    }

    private record PrefixRule(String prefix, String moduleCode) {
    }

    private record Cached<T>(T value, long loadedAt) {
        boolean isFresh(long ttlMs) {
            return System.currentTimeMillis() - loadedAt < ttlMs;
        }
    }

    private volatile Cached<List<PrefixRule>> rules;
    private volatile Cached<Map<String, String>> moduleNames;
    private final Map<Long, Cached<Set<String>>> tenantModules = new ConcurrentHashMap<>();

    /**
     * 解析一个权限码归属的模块。
     *
     * @return 模块编码；无任何前缀命中时返回 {@link Optional#empty()}（调用方应放行）
     */
    public Optional<String> resolveModule(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return Optional.empty();
        }
        String code = permissionCode.trim();
        for (PrefixRule rule : currentRules()) {
            if (code.regionMatches(true, 0, rule.prefix(), 0, rule.prefix().length())) {
                return Optional.of(rule.moduleCode());
            }
        }
        return Optional.empty();
    }

    /**
     * 判断本租户能否使用「要求这些权限码」的接口。
     *
     * @param requireAll {@code true} = 全部码都要满足（AND，{@code @SaCheckPermission} 的默认语义）；
     *                   {@code false} = 满足其一即可（OR，{@code @RequirePermission} 的默认语义）。
     *                   <b>这个参数不能省</b>：OR 语义下"第一个码的模块没开通"不足以拦人 ——
     *                   用户可能靠第二个码进得来，拦了就变成误伤。
     * @return 被挡住的模块；全部通过（或无法判定）时返回 {@link Optional#empty()} 表示放行
     */
    public Optional<BlockedModule> firstBlockedModule(Long tenantId, Collection<String> permissionCodes,
                                                      boolean requireAll) {
        if (tenantId == null || permissionCodes == null || permissionCodes.isEmpty()) {
            return Optional.empty();
        }

        Set<String> valid;
        try {
            valid = currentTenantModules(tenantId);
        } catch (Exception e) {
            // 见类注释第 ③ 条：不确定就放行，但必须留下可排查的痕迹
            log.error("模块 entitlement 判定：读取租户开通记录失败，本次**放行**（fail-open）以避免整租户 403。"
                    + "tenantId={}", tenantId, e);
            return Optional.empty();
        }

        BlockedModule firstBlocked = null;
        boolean anySatisfied = false;
        for (String code : permissionCodes) {
            Optional<String> moduleOpt = resolveModule(code);
            if (moduleOpt.isEmpty()) {
                // 见类注释第 ① 条：无归属前缀 = 不受模块约束，记作"已满足"
                anySatisfied = true;
                continue;
            }
            String moduleCode = moduleOpt.get();
            if (isModuleOpen(valid, moduleCode)) {
                anySatisfied = true;
            } else if (firstBlocked == null) {
                firstBlocked = new BlockedModule(moduleCode, moduleName(moduleCode));
            }
        }

        if (firstBlocked == null) {
            return Optional.empty();
        }
        // AND：任一码的模块没开通就进不来；OR：还有一个码能进来就不拦
        return requireAll || !anySatisfied ? Optional.of(firstBlocked) : Optional.empty();
    }

    /** 平台侧开通/停用某租户的模块后调用，让判定立刻生效（不必等 TTL）。 */
    public void evictTenant(Long tenantId) {
        if (tenantId != null) {
            tenantModules.remove(tenantId);
        }
    }

    /** 平台侧改动了「模块 → 权限码前缀」映射后调用。 */
    public void evictRules() {
        rules = null;
        moduleNames = null;
    }

    // ══════════════════════ 内部 ══════════════════════

    /**
     * 判断某模块对该租户是否已开通。
     *
     * <p>除了精确命中，还接受「开通记录里写的是更细的码」这一历史情况：
     * 迁移前 `sys_tenant_module.module_code` 出现过 `sale:order` 这类细粒度值，
     * 而映射表里的模块编码是 `sale`；两者应视为同一个模块已开通。</p>
     */
    private boolean isModuleOpen(Set<String> validCodes, String moduleCode) {
        if (validCodes.contains(moduleCode)) {
            return true;
        }
        String prefix = moduleCode + ":";
        for (String valid : validCodes) {
            if (valid != null && valid.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> currentTenantModules(Long tenantId) {
        Cached<Set<String>> cached = tenantModules.get(tenantId);
        if (cached != null && cached.isFresh(TENANT_TTL_MS)) {
            return cached.value();
        }
        // getValidModuleCodesStrict：**不吞异常**，失败要能被上面的 catch 看见
        Set<String> loaded = Set.copyOf(tenantModuleService.getValidModuleCodesStrict(tenantId));
        tenantModules.put(tenantId, new Cached<>(loaded, System.currentTimeMillis()));
        return loaded;
    }

    private List<PrefixRule> currentRules() {
        Cached<List<PrefixRule>> cached = rules;
        if (cached != null && cached.isFresh(RULES_TTL_MS)) {
            return cached.value();
        }
        synchronized (this) {
            if (rules != null && rules.isFresh(RULES_TTL_MS)) {
                return rules.value();
            }
            List<PrefixRule> loaded = loadRules();
            rules = new Cached<>(loaded, System.currentTimeMillis());
            return loaded;
        }
    }

    private List<PrefixRule> loadRules() {
        List<SysModulePermission> rows = modulePermissionMapper.selectList(
                new LambdaQueryWrapper<SysModulePermission>()
                        .eq(SysModulePermission::getDeleted, 0));

        // 排序即优先级：前缀越长越优先；同长取 sort 小；再同则按前缀字典序兜底，
        // 保证「同一份数据每次解析结果一致」（不能依赖数据库返回顺序）。
        List<SysModulePermission> ordered = rows.stream()
                .filter(r -> r.getPermissionPrefix() != null && !r.getPermissionPrefix().isBlank()
                        && r.getModuleCode() != null)
                .sorted(Comparator
                        .comparingInt((SysModulePermission r) -> r.getPermissionPrefix().trim().length()).reversed()
                        .thenComparing(SysModulePermission::getSort,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(r -> r.getPermissionPrefix().trim()))
                .toList();

        if (ordered.isEmpty()) {
            log.warn("模块 entitlement：sys_module_permission 无有效映射行 —— 所有「码 → 模块」解析都会落空，门不会拦任何人。"
                    + "若这是新环境，请确认迁移 V11.454.0 已执行。");
        }

        return ordered.stream()
                .map(r -> new PrefixRule(r.getPermissionPrefix().trim(), r.getModuleCode().trim()))
                .toList();
    }

    private String moduleName(String moduleCode) {
        Map<String, String> names = currentModuleNames();
        return names.getOrDefault(moduleCode, moduleCode);
    }

    private Map<String, String> currentModuleNames() {
        Cached<Map<String, String>> cached = moduleNames;
        if (cached != null && cached.isFresh(RULES_TTL_MS)) {
            return cached.value();
        }
        synchronized (this) {
            if (moduleNames != null && moduleNames.isFresh(RULES_TTL_MS)) {
                return moduleNames.value();
            }
            Map<String, String> loaded = new HashMap<>();
            try {
                // 复用 selectInstalledModules：它已 @InterceptorIgnore(tenantLine)，能在租户会话里读到平台注册表
                for (SysModule m : sysModuleMapper.selectInstalledModules()) {
                    if (m.getModuleCode() != null) {
                        loaded.put(m.getModuleCode(), m.getModuleName());
                    }
                }
            } catch (Exception e) {
                // 只是为了 403 文案好看，取不到就回落成模块编码
                log.warn("模块 entitlement：读取模块名称失败，403 文案将回落为模块编码", e);
            }
            moduleNames = new Cached<>(Map.copyOf(loaded), System.currentTimeMillis());
            return moduleNames.value();
        }
    }
}
