package cn.aiedge.tenant.controller;

import cn.aiedge.base.entity.SysTenantModule;
import cn.aiedge.base.vo.Result;
import cn.aiedge.tenant.service.TenantModuleService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 租户模块调用权控制器
 * 管理租户已购买/开通的模块查询
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
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
    public Result<Set<String>> getValidModuleCodes(@RequestParam Long tenantId) {
        return Result.ok(tenantModuleService.getValidModuleCodes(tenantId));
    }

    /**
     * 获取租户的所有模块记录
     */
    @GetMapping("/list")
    @Operation(summary = "获取租户的所有模块记录")
    @SaCheckPermission("system:tenant:query")
    public Result<List<SysTenantModule>> getTenantModules(@RequestParam Long tenantId) {
        return Result.ok(tenantModuleService.getTenantModules(tenantId));
    }
}
