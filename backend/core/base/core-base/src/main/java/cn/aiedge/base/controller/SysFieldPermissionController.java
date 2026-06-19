package cn.aiedge.base.controller;

import cn.aiedge.base.entity.SysFieldPermission;
import cn.aiedge.base.service.SysFieldPermissionService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字段级权限控制器
 * <p>
 * 管理角色对特定表字段的可见性和脱敏规则。
 * 可在前端动态配置，后端序列化时自动应用。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "字段级权限", description = "字段可见性和脱敏规则 CRUD")
@RestController
@RequestMapping("/api/field-permission")
@RequiredArgsConstructor
public class SysFieldPermissionController {

    private final SysFieldPermissionService sysFieldPermissionService;

    @Operation(summary = "分页查询字段权限规则")
    @GetMapping("/page")
    @SaCheckPermission("system:field-permission:list")
    public Result<Page<SysFieldPermission>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) String targetTable) {
        Page<SysFieldPermission> page = new Page<>(pageNum, pageSize);
        Page<SysFieldPermission> result = sysFieldPermissionService.page(page,
                new LambdaQueryWrapper<SysFieldPermission>()
                        .eq(roleId != null, SysFieldPermission::getRoleId, roleId)
                        .eq(targetTable != null && !targetTable.isEmpty(), SysFieldPermission::getTargetTable, targetTable)
                        .orderByDesc(SysFieldPermission::getCreateTime));
        return Result.ok(result);
    }

    @Operation(summary = "查询角色对指定表的字段权限")
    @GetMapping("/list")
    @SaCheckPermission("system:field-permission:list")
    public Result<List<SysFieldPermission>> list(
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) String targetTable) {
        List<SysFieldPermission> list;
        if (roleId != null && targetTable != null) {
            list = sysFieldPermissionService.getByRoleAndTable(List.of(roleId), targetTable);
        } else if (roleId != null) {
            list = sysFieldPermissionService.getByRoleIds(List.of(roleId));
        } else {
            list = sysFieldPermissionService.list();
        }
        return Result.ok(list);
    }

    @Operation(summary = "保存角色字段权限（全量覆盖）")
    @PostMapping("/save")
    @SaCheckPermission("system:field-permission:assign")
    public Result<Void> save(@RequestParam Long roleId, @RequestBody List<SysFieldPermission> permissions) {
        sysFieldPermissionService.saveRoleFieldPermissions(roleId, permissions);
        return Result.ok("保存成功", null);
    }

    @Operation(summary = "删除字段权限规则")
    @DeleteMapping("/{id}")
    @SaCheckPermission("system:field-permission:delete")
    public Result<Void> delete(@PathVariable Long id) {
        sysFieldPermissionService.removeById(id);
        return Result.ok("删除成功", null);
    }
}
