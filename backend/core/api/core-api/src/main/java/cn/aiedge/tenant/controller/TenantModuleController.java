package cn.aiedge.tenant.controller;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.entity.SysTenantModule;
import cn.aiedge.base.vo.Result;
import cn.aiedge.tenant.service.TenantModuleService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

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

    /**
     * 获取当前租户的有效模块编码集合
     */
    @GetMapping("/valid-codes")
    @Operation(summary = "获取当前租户的有效模块编码集合")
    public Result<Set<String>> getValidModuleCodes(@RequestParam(required = false) Long tenantId) {
        return Result.ok(tenantModuleService.getValidModuleCodes(resolveReadableTenantId(tenantId)));
    }

    /**
     * 获取租户的所有模块记录
     */
    @GetMapping("/list")
    @Operation(summary = "获取租户的所有模块记录")
    public Result<List<SysTenantModule>> getTenantModules(@RequestParam(required = false) Long tenantId) {
        return Result.ok(tenantModuleService.getTenantModules(resolveReadableTenantId(tenantId)));
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
