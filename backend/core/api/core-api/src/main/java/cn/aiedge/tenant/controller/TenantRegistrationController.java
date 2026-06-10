package cn.aiedge.tenant.controller;

import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.vo.Result;
import cn.aiedge.tenant.dto.TenantRegisterDTO;
import cn.aiedge.tenant.service.TenantRegistrationService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户注册审批控制器
 * <p>
 * 提供租户自助注册（公开）、管理员审批/驳回等功能。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tenant")
@Tag(name = "租户注册审批", description = "租户自助注册、审批、驳回")
public class TenantRegistrationController {

    private final TenantRegistrationService registrationService;

    /**
     * 租户自助注册（公开接口，无需登录）
     */
    @PostMapping("/register")
    @Operation(summary = "租户自助注册", description = "提交租户注册申请，待系统管理员审批")
    public Result<SysTenant> register(@Valid @RequestBody TenantRegisterDTO.Register dto) {
        SysTenant tenant = registrationService.register(dto);
        log.info("租户注册申请已提交: tenantName={}, contact={}", dto.tenantName(), dto.contactPerson());
        return Result.ok("注册申请已提交，请等待管理员审核", tenant);
    }

    /**
     * 审批通过租户（系统管理员）
     */
    @PostMapping("/{id}/approve")
    @SaCheckLogin
    @SaCheckPermission("system:tenant:approve")
    @Operation(summary = "审批通过租户", description = "系统管理员审批通过租户注册申请，自动初始化租户环境")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestBody(required = false) TenantRegisterDTO.Approve dto) {
        registrationService.approve(id, dto);
        return Result.ok("租户已审批通过");
    }

    /**
     * 驳回租户注册（系统管理员）
     */
    @PostMapping("/{id}/reject")
    @SaCheckLogin
    @SaCheckPermission("system:tenant:approve")
    @Operation(summary = "驳回租户注册", description = "系统管理员驳回租户注册申请")
    public Result<Void> reject(@PathVariable Long id,
                               @Valid @RequestBody TenantRegisterDTO.Reject dto) {
        registrationService.reject(id, dto);
        return Result.ok("租户注册已驳回");
    }

    /**
     * 查询待审核租户列表（系统管理员）
     */
    @GetMapping("/pending")
    @SaCheckLogin
    @SaCheckPermission("system:tenant:approve")
    @Operation(summary = "待审核租户列表", description = "查询所有待审核的租户注册申请")
    public Result<List<SysTenant>> getPendingTenants() {
        List<SysTenant> tenants = registrationService.getPendingTenants();
        return Result.ok(tenants);
    }
}
