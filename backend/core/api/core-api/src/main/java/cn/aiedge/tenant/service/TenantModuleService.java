package cn.aiedge.tenant.service;

import cn.aiedge.base.entity.SysTenantModule;
import cn.aiedge.base.mapper.SysTenantModuleMapper;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.module.mapper.SysModuleMapper;
import cn.aiedge.module.model.SysModule;
import cn.aiedge.tenant.mapper.TenantModuleTombstoneMapper;
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

    /** 「系统」模块：平台级，只属于系统租户（V11.455.0 裁定，见 {@link #assignModule}）。 */
    private static final String SYSTEM_MODULE_CODE = "system";
    /** 系统租户 id：本仓 `admin` 即该租户（`sys_role.SUPER_ADMIN.tenant_id = 1`）。 */
    private static final Long SYSTEM_TENANT_ID = 1L;
    /** purchase_type 缺省值（与 V11.454.0 回填口径一致：不加期限即永久）。 */
    private static final String DEFAULT_PURCHASE_TYPE = "permanent";

    private final SysTenantModuleMapper tenantModuleMapper;
    /** 「含墓碑行」读写：BaseMapper 受 @TableLogic 限制，看不见也改不到软删行，见该 Mapper 类注释。 */
    private final TenantModuleTombstoneMapper tombstoneMapper;
    /** 模块目录（`sys_module`）：开通前必须校验模块码真实存在，防往映射表里塞垃圾码。 */
    private final SysModuleMapper sysModuleMapper;

    /**
     * 获取租户的有效模块编码集合
     * 过滤条件：未删除 + 状态正常 + 未过期
     *
     * <p>读失败时返回空集合（= "没开通"）。调用方若需要区分「真的没开通」与「读不出来」，
     * 用 {@link #getValidModuleCodesStrict(Long)}。</p>
     */
    public Set<String> getValidModuleCodes(Long tenantId) {
        try {
            return getValidModuleCodesStrict(tenantId);
        } catch (Exception e) {
            log.error("获取租户模块调用权失败: tenantId={}", tenantId, e);
            return Collections.emptySet();
        }
    }

    /**
     * 与 {@link #getValidModuleCodes(Long)} 同口径，但**不吞异常**。
     *
     * <p>给模块 entitlement 门用：那道门必须能区分
     * ①「这个租户确实没开通该模块」（→ 403，正常业务结果）与
     * ②「这次读库失败了」（→ 不能当成 ①，否则一次 DB 抖动会把整个租户打成 403）。
     * 原来只有一个吞异常的版本，两种情况在返回值上完全一样。</p>
     */
    public Set<String> getValidModuleCodesStrict(Long tenantId) {
        if (tenantId == null) {
            return Collections.emptySet();
        }

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
     * 为租户开通模块（平台侧，「只增不减」的幂等写入口）。
     *
     * <p><b>幂等 + 复活软删行（平台-MODULE-01 遗留① 的真实缺陷）</b>：
     * 本仓 {@code SysTenantModule.deleted} 带 {@code @TableLogic}，BaseMapper 的
     * {@code selectOne} / {@code updateById} 都看不见也改不到墓碑行（deleted = 1）。
     * 于是「停用后重新开通」在旧写法里会走 else 分支 INSERT 第二行。
     * <b>实测（2026-09-22，`pg_indexes`）</b>：
     * {@code SELECT indexname, indexdef FROM pg_indexes WHERE tablename='sys_tenant_module'}
     * 只返回 {@code sys_tenant_module_pkey}（唯一索引在 id 上），
     * <b>{@code sys_tenant_module} 上没有 (tenant_id, module_code) 唯一索引</b>
     * ⇒ 盲插不会撞键报错，而是**静默留下重复行**（比报错更难发现）。
     * 故这里按实测结论选择「先查含墓碑行的主记录，命中墓碑就复活它」，
     * 而不是「先查有效行、查不到就插」。</p>
     *
     * <p><b>校验（防止越权/脏数据）</b>：① 模块码必须存在于 {@code sys_module} 目录，
     * 否则拒绝（脏码会在 entitlement 前缀解析里变成永远拦不住的洞）；
     * ② {@code system} 是平台级模块，只允许开通给系统租户（tenant_id = 1），
     * 依据 V11.455.0 的裁定（该迁移自检「system 模块的开通租户必须恰好只有 1」）。
     * ③ 本方法**不负责**让 entitlement 门立即生效 —— 缓存失效由调用方
     * （{@code TenantModuleController}）在写成功后调
     * {@code ModuleEntitlementService#evictTenant} 完成：这里不能注入它，
     * 那会形成 {@code ModuleEntitlementService → TenantModuleService} 的循环依赖。</p>
     *
     * @return 落库后的开通记录（复用行或新建行）
     */
    public SysTenantModule assignModule(Long tenantId, String moduleCode,
                                       String purchaseType, LocalDateTime expireTime) {
        if (tenantId == null) {
            throw BusinessException.badRequest("租户ID不能为空");
        }
        SysModule catalog = requireCatalogModule(moduleCode);
        String code = catalog.getModuleCode();

        if (SYSTEM_MODULE_CODE.equals(code) && !SYSTEM_TENANT_ID.equals(tenantId)) {
            throw BusinessException.badRequest(
                    "「系统」模块是平台级模块，只能开通给系统租户（tenant_id=1）；"
                            + "租户级的管理设置请开通「设置」（settings）模块");
        }

        String type = (purchaseType == null || purchaseType.isBlank())
                ? DEFAULT_PURCHASE_TYPE : purchaseType.trim();

        SysTenantModule existing = tombstoneMapper.selectAnyIncludingDeleted(tenantId, code);

        if (existing == null) {
            SysTenantModule module = new SysTenantModule();
            module.setTenantId(tenantId);
            module.setModuleCode(code);
            module.setModuleName(catalog.getModuleName());
            module.setPurchaseType(type);
            module.setExpireTime(expireTime);
            module.setStatus(0);
            module.setDeleted(0);
            tenantModuleMapper.insert(module);
            log.info("开通租户模块: tenantId={}, module={}, type={}", tenantId, code, type);
            return module;
        }

        if (existing.getDeleted() != null && existing.getDeleted() == 1) {
            // 复活墓碑行：显式 SQL（BaseMapper 的 update 会附带 where deleted = 0，改不动墓碑）
            int rows = tombstoneMapper.reviveById(existing.getId(), catalog.getModuleName(), type, expireTime);
            if (rows != 1) {
                throw BusinessException.internalError(
                        "重新开通模块失败：未能复活开通记录 id=" + existing.getId()
                                + "（tenantId=" + tenantId + ", module=" + code + "）");
            }
            log.info("重新开通（复活已停用记录）租户模块: tenantId={}, module={}, id={}, type={}",
                    tenantId, code, existing.getId(), type);
            existing.setDeleted(0);
            existing.setStatus(0);
            existing.setModuleName(catalog.getModuleName());
            existing.setPurchaseType(type);
            existing.setExpireTime(expireTime);
            return existing;
        }

        // 已有有效行 ⇒ 就地更新（幂等：重复开通不新增行、不改主键）
        existing.setModuleName(catalog.getModuleName());
        existing.setPurchaseType(type);
        existing.setExpireTime(expireTime);
        existing.setStatus(0);
        tenantModuleMapper.updateById(existing);
        log.info("更新租户模块调用权: tenantId={}, module={}, id={}, type={}",
                tenantId, code, existing.getId(), type);
        return existing;
    }

    /**
     * 停用（软删）租户模块调用权 —— 与 {@link #assignModule} 成对。
     *
     * <p><b>业务约束</b>：{@code system} 是平台级模块，系统租户（tenant_id = 1）必须保留。
     * V11.455.0 的自检断言「system 模块的有效开通租户恰好只有 1」，
     * 故 {@code remove(1, "system")} 必须拒绝 —— 否则会把平台自己的权限底座关掉，
     * 且让那次迁移的不变式失效。</p>
     *
     * <p>未开通（无有效行）时抛 404 而不是静默成功：平台界面上「停用一个本来就没开的模块」
     * 大概率是操作错对象，明确报错比假装成功更容易定位。</p>
     */
    public void removeModule(Long tenantId, String moduleCode) {
        if (tenantId == null) {
            throw BusinessException.badRequest("租户ID不能为空");
        }
        SysModule catalog = requireCatalogModule(moduleCode);
        String code = catalog.getModuleCode();

        if (SYSTEM_MODULE_CODE.equals(code) && SYSTEM_TENANT_ID.equals(tenantId)) {
            throw BusinessException.badRequest(
                    "「系统」模块是平台级模块，系统租户（tenant_id=1）必须保留，不允许停用");
        }

        SysTenantModule existing = tombstoneMapper.selectAnyIncludingDeleted(tenantId, code);
        if (existing == null || (existing.getDeleted() != null && existing.getDeleted() == 1)) {
            throw BusinessException.notFound("该租户未开通模块：" + code + "（无需停用）");
        }

        tenantModuleMapper.delete(
                new LambdaQueryWrapper<SysTenantModule>()
                        .eq(SysTenantModule::getTenantId, tenantId)
                        .eq(SysTenantModule::getModuleCode, code)
        );
        log.info("停用租户模块调用权: tenantId={}, module={}", tenantId, code);
    }

    /**
     * 校验模块码存在于 {@code sys_module} 模块目录，并返回目录行。
     *
     * <p>复用 {@code SysModuleMapper#selectInstalledModules()}：它已
     * {@code @InterceptorIgnore(tenantLine)}，能在租户会话里读到平台注册表
     * （{@code sys_module.tenant_id} 恒为 0，不忽略则一行都读不出来）。</p>
     */
    private SysModule requireCatalogModule(String moduleCode) {
        if (moduleCode == null || moduleCode.isBlank()) {
            throw BusinessException.badRequest("模块编码不能为空");
        }
        String code = moduleCode.trim();
        return sysModuleMapper.selectInstalledModules().stream()
                .filter(m -> code.equals(m.getModuleCode()))
                .findFirst()
                .orElseThrow(() -> BusinessException.badRequest(
                        "模块编码不存在：" + code + "（未在 sys_module 模块目录中登记，禁止开通）"));
    }
}
