package cn.aiedge.base.controller;

import cn.aiedge.base.service.RoleBillTypeService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 角色-单据类型权限控制器
 * <p>
 * 管理每个角色可访问的单据类型及其操作级别。
 * 借鉴 ql361 的 bill_type 级权限控制模型。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "单据类型权限管理", description = "角色-单据类型权限分配与查询")
@RestController
@RequestMapping("/api/role-bill-type")
@RequiredArgsConstructor
public class RoleBillTypeController {

    private final RoleBillTypeService roleBillTypeService;

    @Operation(summary = "获取角色的单据类型权限列表")
    @GetMapping("/{roleId}")
    @SaCheckPermission("role:detail")
    public Result<List<RoleBillTypeService.BillTypeDetail>> getRoleBillTypes(
            @PathVariable Long roleId) {
        List<RoleBillTypeService.BillTypeDetail> details = roleBillTypeService.getRoleBillTypeDetails(roleId);
        return Result.ok(details);
    }

    @Operation(summary = "获取用户可访问的所有单据类型")
    @GetMapping("/user/{userId}")
    @SaCheckPermission("role:detail")
    public Result<Set<String>> getUserBillTypes(@PathVariable Long userId) {
        return Result.ok(roleBillTypeService.getUserBillTypes(userId));
    }

    @Operation(summary = "分配角色的单据类型权限")
    @PostMapping("/{roleId}")
    @SaCheckPermission("role:assign-permission")
    public Result<Void> assignBillTypes(
            @PathVariable Long roleId,
            @RequestBody List<RoleBillTypeService.BillTypeAssignment> billTypes) {
        roleBillTypeService.assignBillTypes(roleId, billTypes);
        return Result.ok("分配成功", null);
    }

    @Operation(summary = "验证当前用户对单据类型的访问权限")
    @GetMapping("/validate")
    public Result<Void> validateAccess(
            @RequestParam String billType,
            @RequestParam(defaultValue = "1") int requiredLevel) {
        roleBillTypeService.validateBillTypeAccess(billType, requiredLevel);
        return Result.ok("验证通过", null);
    }
}
