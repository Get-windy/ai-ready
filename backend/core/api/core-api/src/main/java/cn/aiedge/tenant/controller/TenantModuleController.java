package cn.aiedge.tenant.controller;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.entity.SysTenantModule;
import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.module.service.ModuleEntitlementService;
import cn.aiedge.tenant.dto.TenantModuleAssignDTO;
import cn.aiedge.tenant.service.TenantModuleService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 租户模块调用权控制器
 * 管理租户已购买/开通的模块查询
 *
 * <p><b>2026-09-18 修复（《应用中心开发文档》§5.4 / §9.2-P0②）</b>：
 * <ul>
 *   <li>原 `/valid-codes` <b>无任何权限注解</b>且接受任意 `tenantId`，
 *       任何登录用户都能读到其它租户开通了哪些模块（越权面）。</li>
 *   <li>原 `/list` 标注的 `system:tenant:query` 在 `sys_permission`（264 行）中
 *       <b>实测 0 行</b> → 除超管（`*` 通配）外所有账号 403，
 *       与设置模块其它页「注解齐全但无权限码」是同一种症状。</li>
 *   <li>处置：<b>租户 id 一律以登录会话为准</b>（`MyBatisPlusConfig#getCurrentTenantIdValue`），
 *       前端传入的 `tenantId` 只在「平台超管（租户隔离豁免）」时才被采纳；
 *       普通租户传入他人租户 id 时<b>忽略该参数并告警</b>，始终返回本租户数据。
 *       不新增权限码 —— `sys_permission` 中 `set:%` / `module:%` 前缀均为 0 行，
 *       凭空新增码而不授予任何角色只会复现同一个 403（同类裁定见
 *       `SetMenuConfigController` 头部注释与《设置模块 README》§5.5）。</li>
 * </ul>
 *
 * <p><b>2026-09-22 新增写入口（平台-MODULE-01 遗留①）</b>：此前
 * {@code TenantModuleService.assignModule()/removeModule()} <b>全仓零调用方</b>，
 * 本控制器与 {@code SetAppCenterController} 都只读，前端模块授权页也只有查询
 * ⇒ entitlement 门（{@code ModuleEntitlementInterceptor}，order 3）**只能拦、不能放**，
 * 平台方无法从界面给租户开关模块，只能直接改库。现补两个平台侧写端点：
 * <ul>
 *   <li>{@code POST /api/tenant-module/assign} —— 开通（幂等，能复活软删行）；</li>
 *   <li>{@code DELETE /api/tenant-module/remove?tenantId=&moduleCode=} —— 停用。</li>
 * </ul>
 * 权限码 {@code system:tenant-module:assign} / {@code system:tenant-module:remove}
 * （种子见迁移 {@code V11.485.0}）。</p>
 *
 * <p><b>为什么除权限码外还要 {@link #assertPlatformAdmin()} 硬校验（不能只靠租户隔离豁免）</b>：
 * 本控制器的写操作作用于「前端传入的任意 tenantId」，而
 * <b>权限码是可以被授予租户角色的</b> —— {@code sys_permission} / {@code sys_role_permission}
 * 都是全局表（在 {@code IGNORE_TENANT_TABLES} 里），租户内分配权限时并不校验码的归属
 * ⇒ 一旦某租户管理员手上有了 {@code system:tenant-module:assign}，他就能给<b>任意租户</b>
 * （含平台租户）开通/停用模块：既能给自己白送一个从未购买的模块（绕过 entitlement 这道商业门），
 * 也能把别家租户的模块关掉。判据与 F-06 给 {@code SysTenantMenuController} 补的那条同源
 * （见该类 {@code assertPlatformAdmin} 的注释），复用同一口径
 * {@code MyBatisPlusConfig.isTenantScopeExempt()}（平台超管的会话豁免标记）。
 * 换言之：<b>权限码只回答「能不能做这件事」，租户隔离豁免才回答「能不能跨租户做」</b>，
 * 两者都要。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/tenant-module")
@SaCheckLogin
@RequiredArgsConstructor
@Tag(name = "租户模块调用权", description = "租户模块的查询和管理")
public class TenantModuleController {

    private final TenantModuleService tenantModuleService;
    /** 开通/停用后必须让它立即生效，见 {@link #assignModule} 的缓存失效说明。 */
    private final ModuleEntitlementService entitlementService;

    /**
     * 获取当前租户的有效模块编码集合
     */
    @SaCheckPermission("system:tenant-module:view")
    @GetMapping("/valid-codes")
    @Operation(summary = "获取当前租户的有效模块编码集合")
    public Result<Set<String>> getValidModuleCodes(@RequestParam(required = false) Long tenantId) {
        return Result.ok(tenantModuleService.getValidModuleCodes(resolveReadableTenantId(tenantId)));
    }

    /**
     * 获取租户的所有模块记录
     */
    @SaCheckPermission("system:tenant-module:view")
    @GetMapping("/list")
    @Operation(summary = "获取租户的所有模块记录")
    public Result<List<SysTenantModule>> getTenantModules(@RequestParam(required = false) Long tenantId) {
        return Result.ok(tenantModuleService.getTenantModules(resolveReadableTenantId(tenantId)));
    }

    /**
     * 为租户开通模块（平台侧）。
     *
     * <p>契约（定死，勿改语义）：请求体
     * {@code {"tenantId": <Long>, "moduleCode": "<String>", "purchaseType": "<可选>", "expireTime": "<可选, ISO 时间>"}}。
     * 幂等：重复调用不新增行（含「停用后重新开通」—— 命中墓碑行就地复活，
     * 见 {@code TenantModuleService#assignModule}）。</p>
     *
     * <p>写成功后调 {@code evictTenant} 主动失效 entitlement 缓存：该判定的租户开通集合
     * 有 30 秒进程内缓存（{@code ModuleEntitlementService.TENANT_TTL_MS}），
     * 不主动失效则「刚停用的模块要等 30 秒才拦得住」—— 对「停用即生效」的平台操作不可接受。
     * 缓存失效放在控制器而不是 Service：Service 不能注入 {@code ModuleEntitlementService}
     * （那会形成它为 {@code ModuleEntitlementService} 的循环依赖）。</p>
     */
    @SaCheckPermission("system:tenant-module:assign")
    @PostMapping("/assign")
    @Operation(summary = "为租户开通模块（仅平台管理员）")
    public Result<Void> assignModule(@Valid @RequestBody TenantModuleAssignDTO.Assign request) {
        assertPlatformAdmin();
        tenantModuleService.assignModule(request.tenantId(), request.moduleCode(),
                request.purchaseType(), parseExpireTime(request.expireTime()));
        entitlementService.evictTenant(request.tenantId());
        log.info("平台侧开通租户模块: tenantId={}, module={}, operator={}",
                request.tenantId(), request.moduleCode(), currentOperator());
        return Result.ok("模块开通成功", null);
    }

    /**
     * 停用租户模块（平台侧）。
     *
     * <p>契约（定死，勿改语义）：{@code DELETE /api/tenant-module/remove?tenantId=<Long>&moduleCode=<String>}。
     * 业务约束：{@code system} 模块只属于系统租户且系统租户必须保留
     * （V11.455.0 裁定）⇒ {@code remove(tenantId=1, moduleCode="system")} 被拒绝并给出中文提示。</p>
     */
    @SaCheckPermission("system:tenant-module:remove")
    @DeleteMapping("/remove")
    @Operation(summary = "停用租户模块（仅平台管理员）")
    public Result<Void> removeModule(@RequestParam Long tenantId, @RequestParam String moduleCode) {
        assertPlatformAdmin();
        tenantModuleService.removeModule(tenantId, moduleCode);
        entitlementService.evictTenant(tenantId);
        log.info("平台侧停用租户模块: tenantId={}, module={}, operator={}", tenantId, moduleCode, currentOperator());
        return Result.ok("模块已停用", null);
    }

    /**
     * 平台管理员硬校验（见类注释：权限码可被授予租户角色，单靠它挡不住跨租户改写）。
     *
     * <p>口径与 F-06 给 {@code SysTenantMenuController} 补的实现完全一致：
     * 只认 {@code MyBatisPlusConfig.isTenantScopeExempt()} —— 该标记在**登录时**写入会话，
     * 不能在这里实时算角色（会经租户拦截器造成无限递归，见该方法注释）。</p>
     */
    private void assertPlatformAdmin() {
        if (!MyBatisPlusConfig.isTenantScopeExempt()) {
            throw BusinessException.forbidden("仅平台管理员可开通/停用租户模块");
        }
    }

    /** 操作人（仅用于日志）。 */
    private String currentOperator() {
        try {
            return StpUtil.getLoginIdAsString();
        } catch (Exception e) {
            return "(未知)";
        }
    }

    /**
     * 解析 ISO 到期时间。契约要求 ISO（如 {@code 2027-01-01T00:00:00}）；
     * 兼容空格分隔（{@code 2027-01-01 00:00:00}）与纯日期（{@code 2027-01-01}，按当天 0 点），
     * 以及带时区偏移的 ISO 串（{@code 2027-01-01T00:00:00+08:00}，转成对应本地时间）。
     *
     * <p>解析失败抛 {@code BusinessException.badRequest}（400 + 明确中文提示）——
     * 不用 RuntimeException：本仓兜底 advice 会把它吞成 500「系统异常」，
     * 前端只能看到一句无从下手的报错。</p>
     */
    private LocalDateTime parseExpireTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignore) {
            // 落到下面的兼容格式
        }
        try {
            return OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (DateTimeParseException ignore) {
            // 落到下面的兼容格式
        }
        try {
            return LocalDateTime.parse(s.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignore) {
            // 落到下面的兼容格式
        }
        try {
            return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (DateTimeParseException ignore) {
            // 四种格式都不匹配
        }
        throw BusinessException.badRequest(
                "到期时间格式不正确：" + raw + "，请使用 ISO 时间（如 2027-01-01T00:00:00）");
    }

    /**
     * 解析「本次查询允许读取的租户 id」。
     *
     * <p>规则：平台超管（租户隔离豁免）可读任意租户；其余账号<b>只读本租户</b>，
     * 传入他人租户 id 时忽略参数并告警（返回本租户数据，不泄露他人信息）。</p>
     */
    private Long resolveReadableTenantId(Long requestedTenantId) {
        Long sessionTenantId = MyBatisPlusConfig.getCurrentTenantIdValue();

        if (MyBatisPlusConfig.isTenantScopeExempt()) {
            // 平台超管：未指定时回落到自身会话租户
            return requestedTenantId != null ? requestedTenantId : sessionTenantId;
        }

        if (sessionTenantId == null) {
            throw new IllegalStateException("无法解析当前会话租户，请重新登录后再试");
        }
        if (requestedTenantId != null && !requestedTenantId.equals(sessionTenantId)) {
            log.warn("租户模块查询：忽略越权的 tenantId 参数，requested={}, session={}",
                    requestedTenantId, sessionTenantId);
        }
        return sessionTenantId;
    }
}
